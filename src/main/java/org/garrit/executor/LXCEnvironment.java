package org.garrit.executor;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

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
    private static final String CONTAINER_NAME_FORMAT = "ge-%02x";
    private static final String SUBMISSIONS_PATH = "garrit/submissions";
    private static final String INPUT_PATH = "garrit/input";

    private final String containerName;
    private final Path containerRoot;

    public LXCEnvironment() throws IOException
    {
        this.containerName = generateContainerName();
        this.containerRoot = Files.createTempDirectory("garrit");

        executeCommand("sudo lxc-create -t garrit -n " + this.containerName + " --dir " + this.containerRoot, 10);
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

        return submissionsRoot;
    }

    @Override
    public Path unpackInput(byte[] input) throws IOException
    {
        Path inputPath = Files.createTempFile(this.containerRoot.resolve(INPUT_PATH), "case-", ".in");

        try (FileOutputStream stream = new FileOutputStream(inputPath.toFile()))
        {
            stream.write(input);
        }

        return inputPath;
    }

    @Override
    public EnvironmentResponse execute(String command, long timeout) throws IOException
    {
        return executeCommand("lxc-execute -n " + this.containerName + " -- " + command, timeout);
    }

    @Override
    public void close() throws IOException
    {
        executeCommand("sudo lxc-destroy -n " + this.containerName, 10);
    }

    private static String generateContainerName() throws IOException
    {
        EnvironmentResponse response = executeCommand("sudo lxc-ls -1", 10);
        List<String> existingContainers = Arrays.asList(response.stdout.split("\n"));

        String containerName;

        do
        {
            containerName = String.format(CONTAINER_NAME_FORMAT, (int) (Math.random() * 255));
        } while (existingContainers.contains(containerName));

        return containerName;
    }

    private static EnvironmentResponse executeCommand(String command, long timeout) throws IOException
    {
        ProcessBuilder builder = new ProcessBuilder("sudo", "lxc-ls", "-1");
        Process process = builder.start();

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
        String stdout = consumeStream(process.getInputStream());
        String stderr = consumeStream(process.getErrorStream());

        return new EnvironmentResponse(exitCode, stdout, stderr);
    }

    private static String consumeStream(InputStream stream)
    {
        try (Scanner scanner = new Scanner(stream))
        {
            return scanner.useDelimiter("\\A").next();
        }
    }
}