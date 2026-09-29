# HDCatalyzer-proxy

This is the repository of the backend. The frontend code can be found in the [HDCatalyzer](https://github.com/IMISE/HDCatalyzer/) repository.

## Setup

1. Clone the repository
   
```sh
git clone https://github.com/mstelbrink/dda-proxy.git
```

2. Set environment variables

```env
CATEGORIES_CONFIG_PATH=/path/to/config
```

### Production

#### docker compose

```sh
docker compose up -d
```

##### Translation Service

```sh
docker run -d -v ollama:/root/.ollama -p 11434:11434 --name ollama ollama/ollama
```

```sh
docker exec -it ollama ollama run translategemma:4b
```

```sh
docker network connect dda-proxy_default ollama # (The network dda-proxy_default works with docker compose and needs to be changed if deployment process uses standard docker commands.)
```

#### docker

```sh
docker build -t dda-proxy .
```

```sh
docker run -p 8080:8080 --mount type=bind,src=<host-path>,dst=/app/config --name DDA_PROXY dda-proxy
```

##### Translation Service
See `docker compose` section