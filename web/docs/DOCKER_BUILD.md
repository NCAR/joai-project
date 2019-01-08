This repo contains a docker file so you can build the jOAI software as a docker image.

The build process will build the WAR for the source in the current repo (even if it's
a dirty working directory) and bundle that with Tomcat, etc.

**Note**: the image contains no pre-configured security so you probably DO NOT want to
expose the container's port untrusted networks.

## Requirements

  1. docker 17.05 or higher (we use multi-stage builds)
  1. this repo cloned to your machine

## How to build

  1. `cd` to the root of this repo
       ```bash
       cd joai-project/
       ```
  1. run the docker build
       ```bash
       export JOAI_VERSION=3.2 # change if needed
       docker build -t ncar/joai-project:$JOAI_VERSION .
       ```


## How to run the built image

  1. start the container with:
      ```bash
      export JOAI_VERSION=3.2 # change if needed
      docker run \
        -d \
        --name=joai \
        -p 8080:8080 \
        -v joai-config:/joai/config \
        -v joai-data:/joai/data \
        ncar/joai-project:$JOAI_VERSION
      ```
  1. open your browser to http://localhost:8080/ (assuming you used 8080 in the previous command)
  1. configure your provider/harvester via the web dashboard

The above example uses named volumes. If you decide to mount the volumes as directories on the host, you might experience
issues because the expected directory structure doesn't exist.


## Running in docker-compose

When running as part of a docker-compose stack, use the following as something to get you started.
Mounting the volumes and exposing the port are probably the two important points.

```yaml
version: '3.4'
services:
  joai:
    image: ncar/joai-project:3.2
    restart: unless-stopped
    volumes:
      - joai_config:/joai/conifg
      - joai_data:/joai/data
    ports:
      - 38080:8080
  
  #...other services

volumes:
  joai_config:
  joai_data:
```


## A note on jOAI server config data
The jOAI app creates config data *inside* the WEB-INF directory, which isn't ideal, but we have that mounted
as a docker volume (`/joai/config`) so your data will be saved even when the container is destroyed.
It's recommended to use a named volume, as in the example commands in this document, so when you run the
container again, it will use the same volume and your data will be there.
If you don't specify a volume, your data won't be lost (the old, detached volume will still exist), but on
subsequent runs you'll get a new volume attached so it will *look* like your data is gone.

You can use a cURL command to do an HTML form submission and configure the jOAI server in a headless fashion.
In the following command we add a *Harvest* Repository:
```bash
curl 'http://localhost:8080/admin/harvester.do' \
 -X POST \
 -F 'shUid=0' \
 -F 'scheduledHarvest=save' \
 -F 'shRepositoryName=Example Source' \
 -F 'shBaseURL=http://www.example.com/knb/dataProvider' \
 -F 'shSetSpec=' \
 -F 'shMetadataPrefix=eml' \
 -F 'shEnabledDisabled=enabled' \
 -F 'shHarvestingInterval=2' \
 -F 'shIntervalGranularity=days' \
 -F 'shRunAtTime=03:00' \
 -F 'shDir=custom' \
 -F 'shHarvestDir=/joai/data/example-source' \
 -F 's=+' \
 -F 'shDontZipFiles=true' \
 -F 'shSet=dontsplit'
```

You could also achieve the same thing for creating a provider. Just perform a form submission in your browser
and look in the browser developer tools to see the HTTP call that was made. Then write a cURL command to do the same.

## A note on where to write the records
Volumes are the answer! You can configure the path where records live in the web dashboard so just be sure to configure
the container to mount that path as a volume. In the example commands in this document, we've mounted `/joai/data` from
the container to a docker volume. So when you configure the jOAI server, be sure to write records to `/joai/data`.
Our example `curl` command above does this.
