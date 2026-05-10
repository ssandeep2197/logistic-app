import { Card } from '@tms/design-system';

export interface AppProps { basePath?: string }

/**
 * Operations MFE — owns loads, customers, carriers, lanes.
 * Phase-1 stub.  Real screens land here as the operations-service domain
 * implementation rolls out.
 */
export default function App(_props: AppProps) {
  return (
    <Card className="p-8">
      <h2 className="text-lg font-semibold">Operations</h2>
      <p className="mt-2 text-sm text-slate-500">
        Loads, customers, carriers, and lanes will live here.  Coming soon.
      </p>
    </Card>
  );
}
