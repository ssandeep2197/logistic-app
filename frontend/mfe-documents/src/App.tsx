import { Card } from '@tms/design-system';

export interface AppProps { basePath?: string }

export default function App(_props: AppProps) {
  return (
    <Card className="p-8">
      <h2 className="text-lg font-semibold">Documents</h2>
      <p className="mt-2 text-sm text-slate-500">Rate-confirmation upload + Claude extraction review queue. Coming soon.</p>
    </Card>
  );
}
