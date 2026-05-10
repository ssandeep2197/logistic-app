import { Card } from '@tms/design-system';

export interface AppProps { basePath?: string }

/**
 * Tracking MFE — Mapbox live truck map, ETAs, geofence alerts. Subscribes
 * to /ws/tracking/positions for real-time updates from tracking-service.
 * Phase-1 stub.
 */
export default function App(_props: AppProps) {
  return (
    <Card className="p-8">
      <h2 className="text-lg font-semibold">Tracking</h2>
      <p className="mt-2 text-sm text-slate-500">
        Live map + ETAs streamed over WebSocket from tracking-service. Coming soon.
      </p>
    </Card>
  );
}
