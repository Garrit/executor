#! /bin/sh

# Install LXC template and sudoers definition for use by Garrit.

if [ `id -u` -ne 0 ]
then
    echo "$0: must be run as root" 1>&2
    exit 1
fi

errors=0

echo "Adding LXC template to library..."
cp lxc-garrit /usr/share/lxc/templates/lxc-garrit

echo
echo -n "Checking validity of sudoers file..."
visudo -cqf lxc-sudoers
if [  $? -eq 0 ]
then
    echo " passed."
    echo "Installing sudoers file..."
    cp lxc-sudoers /etc/sudoers.d/lxc-sudoers
else
    echo " failed!"
    echo "Skipping sudoers file installation. \`visudo -c -f lxc-sudoers\` may provide more insight into the problem."
    errors=1
fi

echo

if [ $errors -eq 0 ]
then
    echo "LXC components for Garrit are good to go."
else
    echo "Looks like something may have gone wrong somewhere along the way."
    echo "Consult any warnings or error messages above for details."
fi
