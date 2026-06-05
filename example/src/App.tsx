import { useEffect, useState, useCallback } from 'react';
import {
  Text,
  View,
  StyleSheet,
  TouchableOpacity,
  ScrollView,
  Alert,
} from 'react-native';
import {
  sendMessage,
  isPaired,
  isReachable,
  isWatchAppInstalled,
  updateApplicationContext,
  onMessageReceived,
  type WearableMessage,
} from '@rn-libs/react-native-wearables';

export default function App() {
  const [paired, setPaired] = useState<boolean | null>(null);
  const [reachable, setReachable] = useState<boolean | null>(null);
  const [watchAppInstalled, setWatchAppInstalled] = useState<boolean | null>(
    null
  );
  const [lastMessage, setLastMessage] = useState<WearableMessage | null>(null);
  const [status, setStatus] = useState('Ready');

  useEffect(() => {
    const unsubscribe = onMessageReceived((message) => {
      setLastMessage(message);
      setStatus('Message received from watch');
    });

    return () => unsubscribe();
  }, []);

  const checkStatus = useCallback(async () => {
    try {
      setStatus('Checking...');
      const [p, r, w] = await Promise.all([
        isPaired(),
        isReachable(),
        isWatchAppInstalled(),
      ]);
      await updateApplicationContext({
        paired: p,
        reachable: r,
        watchAppInstalled: w,
        checkedAt: Date.now(),
      });
      setPaired(p);
      setReachable(r);
      setWatchAppInstalled(w);
      setStatus('Status updated');
    } catch (error: any) {
      setStatus(`Error: ${error.message}`);
    }
  }, []);

  const handleSendMessage = useCallback(async () => {
    try {
      setStatus('Sending message...');
      await sendMessage({
        action: 'hello',
        timestamp: Date.now(),
        greeting: 'Hello from React Native!',
      });
      setStatus('Message sent successfully');
    } catch (error: any) {
      setStatus(`Send failed: ${error.message}`);
      Alert.alert('Send Error', error.message);
    }
  }, []);

  const formatBool = (value: boolean | null) =>
    value === null ? '—' : value ? 'Yes' : 'No';

  return (
    <ScrollView contentContainerStyle={styles.container}>
      <Text style={styles.title}>React Native Wearables</Text>

      <View style={styles.statusCard}>
        <Text style={styles.statusLabel}>Status</Text>
        <Text style={styles.statusText}>{status}</Text>
      </View>

      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Watch Info</Text>
        <View style={styles.row}>
          <Text style={styles.label}>Paired</Text>
          <Text style={styles.value}>{formatBool(paired)}</Text>
        </View>
        <View style={styles.row}>
          <Text style={styles.label}>Reachable</Text>
          <Text style={styles.value}>{formatBool(reachable)}</Text>
        </View>
        <View style={styles.row}>
          <Text style={styles.label}>Watch App Installed (iOS)</Text>
          <Text style={styles.value}>{formatBool(watchAppInstalled)}</Text>
        </View>
      </View>

      {lastMessage && (
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Last Message from Watch</Text>
          <Text style={styles.messageText}>
            {JSON.stringify(lastMessage, null, 2)}
          </Text>
        </View>
      )}

      <View style={styles.buttons}>
        <TouchableOpacity style={styles.button} onPress={checkStatus}>
          <Text style={styles.buttonText}>Check Watch Status</Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={[styles.button, styles.sendButton]}
          onPress={handleSendMessage}
        >
          <Text style={styles.buttonText}>Send Message to Watch</Text>
        </TouchableOpacity>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flexGrow: 1,
    padding: 20,
    paddingTop: 60,
    backgroundColor: '#f5f5f5',
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    textAlign: 'center',
    marginBottom: 20,
    color: '#333',
  },
  statusCard: {
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 16,
    marginBottom: 16,
    shadowColor: '#000',
    shadowOpacity: 0.05,
    shadowRadius: 4,
    shadowOffset: { width: 0, height: 2 },
    elevation: 2,
  },
  statusLabel: {
    fontSize: 12,
    color: '#888',
    textTransform: 'uppercase',
    letterSpacing: 1,
  },
  statusText: {
    fontSize: 16,
    color: '#333',
    marginTop: 4,
  },
  section: {
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 16,
    marginBottom: 16,
    shadowColor: '#000',
    shadowOpacity: 0.05,
    shadowRadius: 4,
    shadowOffset: { width: 0, height: 2 },
    elevation: 2,
  },
  sectionTitle: {
    fontSize: 14,
    fontWeight: '600',
    color: '#666',
    marginBottom: 12,
  },
  row: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingVertical: 6,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: '#eee',
  },
  label: {
    fontSize: 15,
    color: '#333',
  },
  value: {
    fontSize: 15,
    fontWeight: '600',
    color: '#007AFF',
  },
  messageText: {
    fontSize: 13,
    fontFamily: 'monospace',
    color: '#555',
    backgroundColor: '#f9f9f9',
    padding: 8,
    borderRadius: 6,
  },
  buttons: {
    gap: 12,
    marginTop: 8,
  },
  button: {
    backgroundColor: '#007AFF',
    paddingVertical: 14,
    borderRadius: 10,
    alignItems: 'center',
  },
  sendButton: {
    backgroundColor: '#34C759',
  },
  buttonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
  },
});
