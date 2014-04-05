#!/bin/sh
 
# size of swapfile in megabytes
swapsize=512
fallocate -l ${swapsize}M /swapfile
chmod 600 /swapfile
mkswap /swapfile
swapon /swapfile
echo '/swapfile none swap defaults 0 0' >> /etc/fstab
