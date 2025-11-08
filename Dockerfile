FROM ubuntu:latest
LABEL authors="Андрей Овчинников"

ENTRYPOINT ["top", "-b"]