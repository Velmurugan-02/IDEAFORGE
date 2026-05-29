import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

/**
 * IdeaForge WebSocket service using SockJS + STOMP.
 *
 * Usage:
 *   wsService.connect();
 *   const sub = wsService.subscribe('/topic/battle/1', (msg) => { ... });
 *   wsService.unsubscribe(sub);
 *   wsService.disconnect();
 */

const WS_URL = process.env.REACT_APP_WS_URL || 'http://localhost:8080/ws';
const RECONNECT_DELAY_MS = 3000;

let client = null;
let connected = false;
const pendingSubscriptions = []; // subscriptions queued before connection is ready

const wsService = {
  /**
   * Connects to the STOMP broker over SockJS.
   * Auto-reconnects on disconnect after RECONNECT_DELAY_MS.
   */
  connect() {
    if (client && connected) return; // already connected

    client = new Client({
      // SockJS factory — called on each (re)connection attempt
      webSocketFactory: () => {
        const token = localStorage.getItem('token');
        // Pass JWT as query param so the backend STOMP handshake can auth it
        return new SockJS(`${WS_URL}${token ? `?token=${token}` : ''}`);
      },

      reconnectDelay: RECONNECT_DELAY_MS,

      onConnect: () => {
        connected = true;
        console.log('[WS] Connected to IdeaForge broker');

        // Flush any subscriptions that were requested before connection
        pendingSubscriptions.forEach(({ topic, callback, resolve }) => {
          const sub = client.subscribe(topic, (frame) => {
            try {
              callback(JSON.parse(frame.body));
            } catch {
              callback(frame.body);
            }
          });
          resolve(sub);
        });
        pendingSubscriptions.length = 0;
      },

      onDisconnect: () => {
        connected = false;
        console.log('[WS] Disconnected — will attempt reconnect in', RECONNECT_DELAY_MS, 'ms');
      },

      onStompError: (frame) => {
        console.error('[WS] STOMP error:', frame.headers['message']);
      },
    });

    client.activate();
  },

  /**
   * Subscribe to a STOMP topic.
   * Returns a Promise that resolves to the subscription object.
   * If the client isn't connected yet, the subscription is queued.
   *
   * @param {string}   topic    - e.g. '/topic/battle/1'
   * @param {Function} callback - called with the parsed JSON message body
   * @returns {Promise<object>} subscription (call .unsubscribe() to clean up)
   */
  subscribe(topic, callback) {
    return new Promise((resolve) => {
      if (client && connected) {
        const sub = client.subscribe(topic, (frame) => {
          try {
            callback(JSON.parse(frame.body));
          } catch {
            callback(frame.body);
          }
        });
        resolve(sub);
      } else {
        // Queue for when connection is established
        pendingSubscriptions.push({ topic, callback, resolve });
        // Ensure client is connecting
        this.connect();
      }
    });
  },

  /**
   * Unsubscribe from a previously obtained subscription.
   * @param {object} subscription - returned by subscribe()
   */
  unsubscribe(subscription) {
    if (subscription && typeof subscription.unsubscribe === 'function') {
      subscription.unsubscribe();
    }
  },

  /**
   * Send a message to a STOMP destination.
   */
  send(destination, body) {
    if (client && connected) {
      client.publish({
        destination,
        body: JSON.stringify(body),
      });
    }
  },

  /** Cleanly disconnect from the broker. */
  disconnect() {
    if (client) {
      client.deactivate();
      connected = false;
      client = null;
    }
  },

  isConnected() {
    return connected;
  },
};

export default wsService;
