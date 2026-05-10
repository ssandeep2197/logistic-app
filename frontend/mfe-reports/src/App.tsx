import { Card } from '@tms/design-system';

export interface AppProps { basePath?: string }

export default function App(_props: AppProps) {
  return (
    <Card className="p-8">
      <h2 className="text-lg font-semibold">Reports</h2>
      <p className="mt-2 text-sm text-slate-500">
        Daily / weekly / monthly / quarterly / yearly reports plus the custom report builder
        (table, bar, pie). Coming soon.
      </p>
    </Card>
  );
}
