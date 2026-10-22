# Local Mosquitto broker

The Docker Compose stack includes a password-protected Eclipse Mosquitto broker.

## Configure

Copy `.env.docker.example` to `.env`, then replace `MQTT_PASSWORD` with a strong
password. Avoid `$` in this value because Docker Compose treats it as variable
interpolation.

The backend connects over the private Docker network using:

```properties
MQTT_BROKER_URL=tcp://mosquitto:1883
```

An ESP32 or MQTT client on the same LAN connects using the host computer's LAN IP:

```text
Host: <computer-LAN-IP>
Port: 1883
Username: value of MQTT_USERNAME
Password: value of MQTT_PASSWORD
```

Do not expose port 1883 directly to the public Internet. Use TLS or a VPN when
devices must connect from outside the local network.

## Topics

The kit subscribes to:

```text
fire-monitor/kits/{deviceUid}/commands
```

The kit publishes telemetry to:

```text
fire-monitor/kits/{deviceUid}/telemetry
```

Example payload:

```json
{"temperature":35.5,"humidity":70.2,"gas":620,"flame":false}
```

## Start and inspect

```powershell
docker compose up --build
docker compose logs -f mosquitto backend
```

Find the Windows LAN IPv4 address with:

```powershell
Get-NetIPAddress -AddressFamily IPv4 |
  Where-Object { $_.IPAddress -notlike '127.*' -and $_.InterfaceOperationalStatus -eq 'Up' } |
  Select-Object InterfaceAlias, IPAddress
```
