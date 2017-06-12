# Basic Particle Simulation

## Build
```
spatial worker build
```
## Generate snapshot
```
spatial local worker launch snapshotgenerator default ../../../output.snapshot
```
## Launch SpatialOS
```
spatial local launch
```

# Hermes/Spatial-as-a-service/"proxy" server

## How to install and run 

```bash
cd proxy
npm install
node index.js
open http://localhost:3000
```

## Talking to it
(see `proxy/index.js`)
### API
`/start` will start a new simulation (if there isn't already one)

### WebSockets
`request launch`, `request kill`, `console dump`

(ngrok is really useful for exposing the server)