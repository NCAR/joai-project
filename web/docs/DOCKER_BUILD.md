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

The image can be run with a command like the following:
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

Assuming you bound the container to port 8080 as in the above example, you can now access the app in your browser at http://localhost:8080/.

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
      - joai_config:/joai_conifg
      - joai_data:/data
    ports:
      - 38080:8080
  
  #...other services

volumes:
  joai_config:
  joai_data:
```
