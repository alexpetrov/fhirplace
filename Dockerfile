FROM ubuntu:14.04
MAINTAINER Nikolay Ryzhikov <niquola@gmail.com>, Maksym Bodnarchuk <bodnarchuk@gmail.com>

RUN apt-get -qq update
RUN apt-get install -qq -y software-properties-common curl
RUN apt-get -qqy install git build-essential
RUN add-apt-repository ppa:webupd8team/java && apt-get -qq update
RUN echo oracle-java7-installer shared/accepted-oracle-license-v1-1 select true | /usr/bin/debconf-set-selections
RUN apt-get install -qq -y oracle-java7-installer
RUN useradd -d /home/fhir -m -s /bin/bash fhir && echo "fhir:fhir"|chpasswd && adduser fhir sudo

RUN echo 'fhir ALL=(ALL) NOPASSWD: ALL' >> /etc/sudoers

USER fhir
ENV HOME /home/fhir
RUN cd /home/fhir && mkdir -p /home/fhir/bin && curl https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein > /home/fhir/bin/lein && chmod a+x /home/fhir/bin/lein
RUN /home/fhir/bin/lein
env PATH /home/fhir/bin:$PATH

RUN sudo apt-get install -qq -y tmux zsh
RUN cd ~/ && git clone https://github.com/niquola/dotfiles
RUN cd ~/dotfiles && bash install.sh

RUN curl https://raw.githubusercontent.com/creationix/nvm/v0.16.0/install.sh | bash
RUN bash -lc 'source ~/.nvm/nvm.sh && nvm install 0.10'

COPY . /home/fhir/fhirplace
RUN sudo chown -R fhir:fhir /home/fhir/fhirplace
RUN cd ~/fhirplace && git submodule init && git submodule update
RUN cd ~/fhirplace && lein deps
RUN cd ~/fhirplace && cp lein-env.tpl .lein-env && lein javac
RUN cd ~/fhirplace && cp dev/production.clj dev/user.clj
RUN mkdir -p ~/fhirplace/resources/public/app
RUN sudo ln -s ~/fhirplace/resources/public/app /app

RUN sudo apt-get -qqy install nginx
RUN sudo cp ~/fhirplace/nginx.conf /etc/nginx/sites-available/default

EXPOSE 80

CMD sudo service nginx restart && cd ~/fhirplace && env FHIRPLACE_SUBNAME="//$DB_PORT_5432_TCP_ADDR:$DB_PORT_5432_TCP_PORT/fhirbase" lein repl
