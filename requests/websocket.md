# Start live kit readings

There is no reading cache. Each incoming MQTT reading is mapped by ModelMapper
and forwarded immediately to the house WebSocket topic.

## Frontend sequence

1. Connect STOMP over WebSocket to `ws://localhost:8080/ws` (`wss://` with HTTPS).
2. Supply `Authorization: Bearer <JWT>` in the STOMP CONNECT headers when security is enabled.
3. Subscribe to `/topic/houses/{houseId}/data` before requesting telemetry.
4. Call `POST /me/house/kit` with the same Bearer token in the HTTP header. No IDs or body are required.
5. Update the matching location row for each incoming JSON `DataResponse`.

The POST is handled by `KitController`. `KitService.getMyHouseKits()` gets the user ID
from the security context and finds their houses through `house_members` (for both
MEMBER and OWNER users). For each distinct house it calls private `getData(houseId)`:
House -> Locations -> Kits ->
MQTT START commands. HTTP 202 returns requested kit IDs, e.g. `[1, 2]`, not readings.
The `/me/house/kit` endpoint finds houses through `house_members`.
Admins can instead call `POST /house/{houseId}/kit` with a Bearer token to start
telemetry for one selected house without membership. `getHouseKit(houseId)` checks
`ROLE_ADMIN` in the security context before calling `getData(houseId)`. Non-admins
receive 403; a missing house returns 404. Admins can also subscribe to any existing
house topic; other users must be members.
The user ID comes from the JWT, never from request parameters. A user with no house
receives 404. Empty houses return `[]`, and
unavailable MQTT returns 503. `getData` is an internal service call; the old
`/houses/{houseId}/data` HTTP endpoint is removed.

202 confirms publishing the commands to the broker, not kit receipt or readiness.
If publishing fails partway, earlier kits may already have started. Firmware
must handle repeated START commands idempotently (do not spawn duplicate loops).

## Kit firmware contract

The kit stays connected to the MQTT broker and subscribes to:

`fire-monitor/kits/{kitId}/commands`

BE sends this non-retained command with QoS 1:

```json
{"command":"START_TELEMETRY"}
```

The kit then publishes readings continuously to:

`fire-monitor/kits/{kitId}/telemetry`

```json
{"temperature":35.5,"humidity":70.2,"gas":620,"flame":false}
```

The BE cannot make firmware that does not support this command start sending.
Stopping transmission on the last viewer's departure is not implemented in this step.
Local fire detection and alarm reporting must remain active without viewers.

## WebSocket client example

With `@stomp/stompjs`:

```javascript
import { Client } from '@stomp/stompjs';

const client = new Client({
  brokerURL: 'ws://localhost:8080/ws',
  connectHeaders: { Authorization: `Bearer ${token}` },
  reconnectDelay: 3000,
  heartbeatIncoming: 10000,
  heartbeatOutgoing: 10000,
});
client.onConnect = async () => {
  client.subscribe(`/topic/houses/${houseId}/data`, message => {
    const data = JSON.parse(message.body);
    console.log(data); // Update row matching data.location.
  });
  try {
    const response = await fetch('http://localhost:8080/me/house/kit', {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
    });
    if (!response.ok) throw new Error(`Cannot start telemetry: ${response.status}`);
    console.log('Requested kits:', await response.json());
  } catch (error) {
    console.error(error); // Show request failure to the viewer.
  }
};
client.onStompError = frame => console.error(frame.headers.message);
client.activate();
// On page unmount/logout: await client.deactivate();
```

No snapshots or replay are available. Show 'waiting for data' until fresh readings arrive,
and show a disconnected/stale state if updates stop. STOMP subscription and HTTP
are separate requests; early readings can be missed while subscription is registering.
Subsequent continuous readings update the screen.
Only configured CORS origins may connect. With security enabled, WebSocket subscriptions
require membership or the ADMIN role. The local `APP_SECURITY_ENABLED=false` setting
bypasses WebSocket authentication; use `true` for membership enforcement. HTTP `/me/house/kit`
always requires a JWT identity and membership.
Browser SEND frames are rejected. WebSocket subscriptions are local to one BE instance.
Status remains the requested fixed `NORMAL`; fire detection and event persistence
are not implemented by this change.
