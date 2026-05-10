import { Card } from '@tms/design-system';

export interface AppProps { basePath?: string }

/**
 * Dispatch MFE — drag-drop board, auto-scheduler suggestions, driver comms.
 * Phase-1 stub.
 */
export default function App(_props: AppProps) {
  return (
    <Card className="p-8">
      <h2 className="text-lg font-semibold">Dispatch board</h2>
      <p className="mt-2 text-sm text-slate-500">Trucks ↔ loads assignment surface. Coming soon.</p>
    </Card>
  );
}
