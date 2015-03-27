package org.garrit.executor;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.garrit.common.messages.SubmissionFile;

/**
 * An execution environment which uses <a
 * href="https://linuxcontainers.org/">Linux containers</a> to provide isolation
 * of the execution environment.
 *
 * @author Samuel Coleman <samuel@seenet.ca>
 * @since 1.0.0
 */
public class LXCEnvironment extends ExecutionEnvironment
{
    private static final String CONTAINER_NAME_FORMAT = "garrit-exec-%02x";
    private static final String SUBMISSIONS_PATH = "garrit/submission";
    private static final String INPUT_PATH = "garrit/input";

    private static final int LXC_ADMIN_TIMEOUT = 10;

    private final String containerName;
    private final Path containerRoot;

    public LXCEnvironment() throws IOException
    {
        this.containerName = generateContainerName();
        this.containerRoot = Files.createTempDirectory("garrit");

        executeCommand(
                Arrays.asList(
                        "sudo", "lxc-create", "-t", "garrit", "-n", this.containerName, "--dir",
                        this.containerRoot.toString()),
                null,
                LXC_ADMIN_TIMEOUT);
    }

    @Override
    public Path unpack(List<SubmissionFile> files) throws IOException
    {
        Path submissionsRoot = this.containerRoot.resolve(SUBMISSIONS_PATH);

        for (SubmissionFile file : files)
        {
            Path submissionPath = submissionsRoot.resolve(file.getFilename());
            try (FileOutputStream stream = new FileOutputStream(submissionPath.toFile()))
            {
                stream.write(file.getContents());
            }
        }

        return Paths.get("/").resolve(SUBMISSIONS_PATH);
    }

    @Override
    public Path unpackInput(byte[] input) throws IOException
    {
        Path inputPath = Files.createTempFile(this.containerRoot.resolve(INPUT_PATH), "case-", ".in");

        try (FileOutputStream stream = new FileOutputStream(inputPath.toFile()))
        {
            stream.write(input);
        }

        return Paths.get("/").resolve(INPUT_PATH);
    }

    @Override
    public EnvironmentResponse execute(List<String> command, String input, long timeout) throws IOException
    {
        ArrayList<String> wrappedCommand = new ArrayList<>(command.size() + 5);
        wrappedCommand.addAll(Arrays.asList("sudo", "lxc-execute", "-n", this.containerName, "--"));
        wrappedCommand.addAll(command);

        return executeCommand(wrappedCommand, input, timeout);
    }

    @Override
    public void close() throws IOException
    {
        executeCommand(Arrays.asList("sudo", "lxc-destroy", "-n", this.containerName),
                null,
                LXC_ADMIN_TIMEOUT);
    }

    private static String generateContainerName() throws IOException
    {
        EnvironmentResponse response = executeCommand(Arrays.asList("sudo", "lxc-ls", "-1"),
                null,
                LXC_ADMIN_TIMEOUT);
        List<String> existingContainers = Arrays.asList(response.stdout.split("\n"));

        String containerName;

        do
        {
            containerName = String.format(CONTAINER_NAME_FORMAT, (int) (Math.random() * 255));
        } while (existingContainers.contains(containerName));

        return containerName;
    }

    private static EnvironmentResponse executeCommand(List<String> command, String input, long timeout)
            throws IOException
    {
        ProcessBuilder builder = new ProcessBuilder(command);
        Process process = builder.start();

        StreamConsumer stdoutConsumer = new StreamConsumer(process.getInputStream());
        StreamConsumer stderrConsumer = new StreamConsumer(process.getErrorStream());

        stdoutConsumer.start();
        stderrConsumer.start();

        if (input != null)
        {
            BufferedOutputStream stdinStream = new BufferedOutputStream(process.getOutputStream());
            stdinStream.write(input.getBytes());
            stdinStream.close();
        }

        boolean finished;
        try
        {
            finished = process.waitFor(timeout, TimeUnit.SECONDS);
        }
        catch (InterruptedException e)
        {
            process.destroyForcibly();
            throw new IOException("Interrupted while waiting for child process", e);
        }

        if (!finished)
        {
            throw new IOException("Child process failed to complete in a timely manner");
        }

        int exitCode = process.exitValue();
        String stdout = stdoutConsumer.getConsumed();
        String stderr = stderrConsumer.getConsumed();

        process.destroy();

        return new EnvironmentResponse(exitCode, stdout, stderr);
    }

    @RequiredArgsConstructor
    @Slf4j
    private static class StreamConsumer extends Thread
    {
        private final InputStream stream;
        private final StringBuilder builder = new StringBuilder();

        @Override
        public void run()
        {
            try
            {
                BufferedInputStream bufferedStream = new BufferedInputStream(this.stream);

                int c;
                while ((c = bufferedStream.read()) >= 0)
                    this.builder.append((char) c);

                this.stream.close();
            }
            catch (IOException e)
            {
                log.error("Failure consuming stream", e);
            }
        }

        /**
         * @return get the output of the stream so far
         */
        public String getConsumed()
        {
            return this.builder.toString();
        }
    }
}