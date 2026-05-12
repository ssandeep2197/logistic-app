/**
 * Maps the OAuth error codes the backend sends back via
 * {@code ?oauth_error=…} to user-facing messages.
 */
const MESSAGES: Record<string, string> = {
  oauth_not_configured:   'Google sign-in is not enabled on this deployment yet.',
  oauth_bad_mode:         'Internal error: invalid OAuth mode.',
  oauth_missing_tenant:   'Pick or enter your tenant before signing in with Google.',
  oauth_bad_state:        'Sign-in took too long. Please try again.',
  oauth_missing_params:   'Google returned an incomplete response. Please try again.',
  oauth_exchange_failed:  'Google rejected the sign-in. Please try again.',
  oauth_no_id_token:      'Google did not return an identity token. Please try again.',
  oauth_transport:        'Could not reach Google. Check your connection and try again.',
  oauth_decode:           'Got a malformed response from Google. Please try again.',
  oauth_bad_audience:     'OAuth client mismatch — please contact support.',
  oauth_email_unverified: 'Your Google account email is not verified.',
  oauth_no_account:       'No Helloworlds account is linked to this Google identity. Sign up first.',
  oauth_wrong_tenant:     'This Google account is linked to a different tenant.',
  oauth_tenant_missing:   'The tenant linked to this account no longer exists.',
  oauth_user_missing:     'The user linked to this Google account no longer exists.',
  oauth_unexpected:       'Something unexpected happened during sign-in. Please try again.',
  google_access_denied:   'You declined access on the Google consent screen.',
  conflict:               'That tenant slug is already taken — pick another and try again.',
};

export function oauthErrorMessage(code: string): string {
  return MESSAGES[code] ?? `Sign-in failed (${code}).`;
}
