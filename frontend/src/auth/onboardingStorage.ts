const ONBOARDING_SEEN_KEY = 'onboarding-seen'

export function hasSeenOnboarding(): boolean {
  return localStorage.getItem(ONBOARDING_SEEN_KEY) === 'true'
}

export function markOnboardingSeen(): void {
  localStorage.setItem(ONBOARDING_SEEN_KEY, 'true')
}
