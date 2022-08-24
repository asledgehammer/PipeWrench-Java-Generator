# Project Zomboid Java API Typescript Definition Generator

## Run instructions

### MacOS

#### Prerequisites

- steamcmd : `brew install --cask steamcmd`
- javasdk : `brew install java`
- gradle : `brew install gradle`

#### Run

Download pz dedicated server to a local directory using steamcd

```shell
bash scripts/install_pz.sh
```

Grab the compiled java byte-code files + jar files from the dedicated server distribution
and throw them into a jar file so we can use them as dependencies

```shell
bash scripts/prep_libs.sh
```

Now you can run the generator and create those type defs!

```shell
gradle run --args ./dist
```
