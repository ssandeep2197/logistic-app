/**
 * Placeholder rendered for routes whose MFE hasn't been deployed yet.
 * Swap each usage in App.tsx for a real lazy(import('mfe_<name>/App'))
 * once that MFE is built and listed in vite.config.ts remotes.
 */
export function ComingSoon({ name }: { name: string }) {
  return (
    <div className="rounded-lg border border-slate-200 bg-white p-8 shadow-sm">
      <h2 className="text-lg font-semibold text-slate-900">{name}</h2>
      <p className="mt-2 text-sm text-slate-500">
        This feature is not yet deployed. Check back after the {name.toLowerCase()} MFE rolls out.
      </p>
    </div>
  );
}
