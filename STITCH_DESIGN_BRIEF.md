# DuoSport — Google Stitch Design Brief

How to use: paste the **Master Context** into Stitch first (or prepend it to
every prompt), then generate screens one at a time using the per-screen
prompts. Every screen below maps 1:1 to data the backend actually serves —
nothing here is speculative.

---

## MASTER CONTEXT (prepend to every Stitch prompt)

> DuoSport is a mobile app (iOS/Android, design at 390×844) that combines
> sports-venue booking with social matchmaking. Players discover courts near
> them, book by the hour, log match results against friends, and build a
> per-sport skill rating (Elo) and reliability score. Venue owners get a
> "host mode" with an operations dashboard: revenue, occupancy heatmaps,
> booking approvals, and "Smart Fill" — one tap notifies nearby matching
> players about an empty slot.
>
> Brand personality: energetic, competitive but friendly. Think Strava meets
> Airbnb. Modern athletic style: bold rounded sans-serif type, generous
> whitespace, vivid accent color (electric green #16C172 or similar) on a
> near-white background, dark charcoal text, soft shadows, large rounded
> cards (16px radius), pill-shaped chips and buttons. Subtle use of a second
> accent (deep navy) for the host/vendor mode so the two modes feel related
> but distinct. Avoid corporate stiffness; avoid childish gamification.
>
> Two user modes sharing one app: PLAYER (green accent) and HOST/venue owner
> (navy accent). Bottom tab bar in player mode: Home, Explore, Book,
> Matches, Profile. Host mode swaps to: Dashboard, Schedule, Requests,
> Courts, Profile.

---

## PLAYER MODE SCREENS

### 1. Welcome / Onboarding
Design a mobile onboarding flow (3 swipeable slides) for DuoSport.
Slide 1: "Find your court" — venue discovery near you. Slide 2: "Find your
people" — matchmaking by skill level. Slide 3: "Track your rise" — Elo
rating per sport. Energetic action photography placeholders, bold headline
per slide, page dots, a primary "Get started" button and a "Log in" text
link. Last slide asks for location permission with a friendly explanation
("We use your location to find courts near you").

### 2. Sign Up
Mobile sign-up screen: first name, last name, username (lowercase hint),
email, password with live rule checklist (8+ chars, uppercase, lowercase,
number, special character), confirm password. Prominent "Continue with
Google" button above the form with a divider ("or sign up with email").
Small print links for terms. Clean single-column form, floating labels.

### 3. Log In + Forgot Password
Login screen: email + password, "Continue with Google" button, "Forgot
password?" link. Forgot-password flow as two follow-up screens: (a) enter
email, (b) enter a 6-digit verification code in six separate boxes with a
countdown to resend, then set a new password.

### 4. Home Feed (the most important screen)
Mobile home screen with three stacked sections over a scrollable feed:
(a) "Your sports" — horizontal chip row; each chip shows sport icon, sport
name, and the player's Elo (e.g. "Tennis · 1450"); the primary sport chip
has a filled accent style with a small star; tapping a chip filters the
feed. (b) "Courts near you" — vertical list of venue cards: venue name,
distance ("1.2 km"), small row of sport icons available there, chevron.
Cards use a map-pin thumbnail (no photos available). (c) "Discover nearby"
— horizontal strip of sport chips the user does NOT play yet but which
have courts nearby ("Padel · 3 venues near you"), each with a subtle
"+ Add" affordance. Top bar: location name, notification bell with unread
badge. Pull-to-refresh.

### 5. Explore / Venue Search
Search screen: search bar (by venue name), filter chips below it (sport,
distance radius slider 1–50 km, open now). Results as the same venue cards
from Home, sorted by distance, with a toggle between list view and map view
(map shows venue pins around the user's location dot).

### 6. Venue Detail
Venue detail screen: header with venue name, address, distance, phone,
description. "Open today 06:00–22:00" hours row (expandable to full week).
Amenities as small pill chips. Then a "Courts" section: one card per court
showing court number, sport icon + name, indoor/outdoor tag, surface type,
hourly rate ("$25/hr"), and a "See availability" button. Sticky bottom CTA:
"Book a court".

### 7. Availability Grid + Booking Flow
Court booking screen: horizontal date picker (next 14 days), then a
vertical time grid for the selected day showing hour slots from open to
close. Slot states: available (white, tappable), booked (greyed),
blocked-by-venue (striped), selected (accent fill). Multi-hour selection by
tapping consecutive slots. Bottom summary bar: selected time range,
computed price, and either "Book now" (instant-book venues) or "Request to
book" (approval venues — show a small "Owner approves within 24h" hint).
Optional notes field ("Anything the venue should know?"). Confirmation
sheet with booking summary after submit; for requests, status shows
"Pending approval".

### 8. My Bookings
Two tabs: Upcoming and Past. Upcoming cards: venue name, court number,
date + time range, status pill (Confirmed = green, Pending = amber),
price, cancel button (opens a sheet asking for an optional reason and
showing the refund hint). Past tab adds statuses Completed / Cancelled /
Declined / No-show (red pill) and a "Log match result" shortcut button on
completed bookings.

### 9. Log a Match (manual entry)
Multi-step form sheet: (1) pick sport (chip grid), (2) pick date,
(3) build teams — "Your team" and "Opponents" with friend search to add
players; allow adding an external opponent by email with a note "We'll
invite them to confirm"; (4) score entry ("6-4, 6-3" free text with format
hint) and winner selection (Team A / Team B / Draw); (5) review + submit.
Show a banner: "Opponents must verify the result before it counts toward
your rating."

### 10. Match History + Match Detail
History: vertical timeline of match cards — sport icon, opponent name(s),
score, win/loss/draw left-border color coding, date, verification status
pill (Pending / Confirmed / Disputed). Detail screen adds: full team
rosters, a photo gallery strip ("Memories") where any participant can add
photos via URL with caption, and — for the opposing team only — Verify /
Dispute buttons with a confirmation dialog.

### 11. Stats
Personal stats screen with a sport switcher at top (chips). Cards: overall
record (W-L, win %), current streak with a flame icon, monthly performance
mini bar-chart, Elo trend sparkline, head-to-head list ("vs Keshav: 3–2",
tappable), most-played opponents leaderboard. Keep charts simple and bold.

### 12. Friends
Three tabs: Friends (list with avatar, name, mutual count, unfriend via
long-press), Requests (received with Accept/Decline buttons; sent with
Cancel), Find (search bar + "Suggested for you" list with mutual-friend
counts and Add button). Friend request sheet allows an optional message
(max 500 chars).

### 13. Notifications
Notification feed: avatar or type icon, message, relative time, unread dot;
"Mark all read" text button in header. Visually distinct types: friend
request (person icon), match invite/verify (trophy), booking confirmed/
declined/cancelled (calendar), Smart Fill offer (lightning bolt, accent
background — these deep-link to the booking grid), no-show notice (red
alert style with "Contact venue" hint).

### 14. Profile & Per-Sport Preferences
Own profile: avatar, name, username, bio, gender/age (optional), stats
summary row. "My sports" section: card per sport showing Elo, matches
played, win rate, primary-sport star toggle. Tapping a sport opens its
preferences editor: proficiency level (segmented control), years playing,
preferred format (singles/doubles/team), opponent filters (age range
double slider, Elo range double slider, gender preference), availability
toggles (weekdays / weekends), travel radius slider, "Open to matchmaking"
master switch, notification radius slider, and Delete sport. Settings
gear: account (change password with current-password field, logout,
"Log out of all devices"), notification toggles.

### 15. Public Player Profile
Another player's profile: avatar, name, username, bio, per-sport Elo
badges, overall record, recent matches list, mutual friends row, primary
CTA "Add friend" (or status pill if pending/friends), secondary
"Challenge" button that pre-fills the Log Match flow.

---

## HOST MODE SCREENS (navy accent)

### 16. Become a Host
Single marketing-style screen: "List your venue on DuoSport" with three
benefit bullets (fill dead hours automatically, no-show protection,
real-time revenue analytics) and a "Become a host" CTA. After upgrade, a
success state explains the app now has a mode switcher in the profile tab.

### 17. Owner Overview (multi-venue rollup)
Host dashboard landing: big revenue numbers (this week / this month),
stat row (bookings today, no-shows 30d, cancellations 30d), then a card
per venue (name, monthly revenue, today's booking count) sorted by
revenue, each tappable. Floating "+ Add venue" button.

### 18. Venue Dashboard
Single-venue operations screen with section cards: (a) revenue this
week/month; (b) today at a glance (bookings count); (c) leakage card —
no-shows and cancellations in 30 days with a subtle red tint; (d) upcoming
bookings list: time, court, player name + a small reliability badge
(EXCELLENT green / GOOD teal / FAIR amber / POOR red / NEW grey) and
payment status; (e) court utilization bars (booked hours per court, 30d).
Header actions: heatmap icon, revenue-report icon, CSV export icon.

### 19. Occupancy Heatmap
Full-screen 7-column (Mon–Sun) × hourly-row grid; cell color intensity =
bookings in the last 30 days; empty cells are visually "cold" (light
grey). Legend underneath. Tapping a cold cell opens the Smart Fill sheet
pre-filled with that day/time. Window selector: 30 / 90 days.

### 20. Revenue Report ("money left on the table")
Per-court table: court name, hourly rate, booked hours, estimated empty
hours, missed revenue (bold red number). Venue total at top as a headline
("$1,240 left on the table this month"). Each row has a "Fill these hours"
button that opens Smart Fill. One-line method note at the bottom.

### 21. Smart Fill
Bottom sheet: court picker, date + time-range picker, optional discount %
stepper, then a preview line ("We'll notify up to 50 matching players
within their travel radius — closest first"). Submit shows a success state:
"17 players notified" with a lightning-bolt animation. Show remaining
daily quota ("2 of 3 slots left today"). Error state for the daily cap.

### 22. Schedule + Booking Requests
Calendar-style schedule: day selector, list of bookings grouped by court
with status pills; long-press a past confirmed booking to "Mark as
no-show" (confirmation dialog warns the player will be notified).
Requests tab with a badge count: pending request cards showing player
name + reliability badge, court, time, price, and Approve / Decline
buttons (decline asks an optional reason). Court-block creation: "+ Block
time" button opening a sheet (court, time range, reason e.g. maintenance).

### 23. Courts & Venue Management
Venue settings: edit venue details (name, address, phone, description,
opening hours per weekday, amenities chips), court list with add/edit
(court number, sport, hourly rate, indoor toggle, surface type), and a
booking-policy section (instant book vs approval required, cancellation
policy).

---

## DESIGN SYSTEM NOTES FOR STITCH

- Status pill colors used consistently everywhere:
  Confirmed/Win = green, Pending = amber, Cancelled/Declined = grey,
  No-show/Loss/Disputed = red, Completed = navy.
- Reliability badges: EXCELLENT (green), GOOD (teal), FAIR (amber),
  POOR (red), NEW (grey outline).
- Elo always shown as "Sport · 1450" chips; primary sport gets a star.
- Distances always "1.2 km" format.
- Venues have NO photos in v1 — use map-pin thumbnails, sport-icon
  collages, or color blocks. (Match "Memories" photos DO exist.)
- Money formatted "$25" or "$25/hr"; revenue numbers large and bold.
- Empty states matter: no preferences yet → "Add your first sport" hero;
  no bookings → "Find a court near you" CTA; no friends → invite link.
- Dark mode optional; if generated, keep accent colors identical.
