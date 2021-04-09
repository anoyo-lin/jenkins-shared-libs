FROM centos:7

RUN adduser devops -u 9001 -U && \
usermod -aG wheel devops && \
rm /etc/localtime && \
ln -s /usr/share/zoneinfo/Asia/Hong_Kong /etc/localtime

RUN yum install -y sudo

WORKDIR /home/devops

COPY sudoers /etc/sudoers
COPY helm /usr/local/bin/helm
COPY kubectl /usr/local/bin/kubectl
COPY docker /usr/local/bin/docker
COPY script.sh /usr/local/bin/script.sh

USER devops
RUN mkdir -p /home/devops/.ssh/
COPY --chown=devops:devops known_hosts /home/devops/.ssh/known_hosts
COPY --chown=devops:devops id_rsa /home/devops/.ssh/id_rsa
RUN ls -al ${HOME}/.ssh/

RUN chmod 700 ${HOME}/.ssh && \
    sudo chmod 600 ${HOME}/.ssh/id_rsa && \
    chmod 600 ${HOME}/.ssh/known_hosts && \
    chown devops:devops ${HOME}/.ssh && \
    sudo chmod 755 /usr/local/bin/helm && \
    sudo chmod 755 /usr/local/bin/kubectl && \
    sudo chmod 755 /usr/local/bin/docker && \
    sudo chmod 755 /usr/local/bin/script.sh && \
    mkdir -p /home/devops/.kube

COPY kubeconfig /home/devops/.kube/config


ENTRYPOINT ["/usr/local/bin/script.sh"]



#!/bin/bash
IP=$(sed -n '/6443/p' /home/devops/.kube/config |awk -F: '{print $3}'| cut -d/ -f3)
echo "${IP}  kcr.test.io | sudo tee -a /etc/hosts
sudo chmod 777 /var/run/docker.sock
helm repo add gene_test http://${IP}:30002/library/helm/
helm repo update


https://blog.csdn.net/weiwei9363/article/details/78570390
