-- ============================================================================
-- Stress-test schema for DuoSport / GameApp.
--
-- Mirrors the Hibernate-generated schema (ddl-auto: update with Spring's
-- CamelCaseToUnderscoresNamingStrategy): same tables, column names, types,
-- and EXACTLY the indexes/uniques the entities declare — nothing more.
-- Two deliberate substitutions, called out inline:
--   * venues.location is geometry(Point,4326) in prod (PostGIS). Stubbed as
--     text here so the harness runs on stock Postgres; the two spatial
--     queries are analyzed separately in RESULTS.md.
--   * Hibernate emits numeric(38,2) for BigDecimal; kept as-is.
-- ============================================================================

DROP TABLE IF EXISTS smart_fill_offers, user_preferences, participants, matches,
  password_reset_codes, refresh_tokens, notifications, friendships, court_blocks,
  booking_policies, bookings, courts, sports, venues, users CASCADE;

CREATE TABLE users (
  id uuid PRIMARY KEY,
  first_name varchar(255) NOT NULL,
  last_name varchar(255) NOT NULL,
  email varchar(255) NOT NULL UNIQUE,
  password varchar(255) NOT NULL,
  phone_number varchar(255) UNIQUE,
  joining_date timestamp,
  role varchar(255) NOT NULL,
  updated_at timestamp NOT NULL,
  is_verified boolean NOT NULL,
  verification_token varchar(255),
  verification_token_expiry timestamp,
  username varchar(255) UNIQUE,
  bio varchar(500),
  profile_picture_url varchar(255),
  gender varchar(255),
  date_of_birth date,
  expo_push_token varchar(255),
  email_notifications_enabled boolean NOT NULL,
  push_notifications_enabled boolean NOT NULL,
  google_id varchar(255) UNIQUE
);

CREATE TABLE venues (
  id uuid PRIMARY KEY,
  owner_id uuid NOT NULL REFERENCES users(id),
  name varchar(255) NOT NULL,
  address text NOT NULL,
  description text,
  opening_hours jsonb,
  amenities jsonb,
  -- prod: geometry(Point, 4326); stubbed for stock Postgres
  location text,
  is_active boolean NOT NULL DEFAULT true,
  created_at timestamp
);

CREATE TABLE sports (
  id uuid PRIMARY KEY,
  sport_name varchar(255),
  min_players int NOT NULL,
  max_players int NOT NULL,
  icon_url varchar(255),
  description varchar(255),
  is_active boolean NOT NULL,
  scoring_type varchar(255)
);

CREATE TABLE courts (
  id uuid PRIMARY KEY,
  venue_id uuid NOT NULL REFERENCES venues(id),
  sports_id uuid NOT NULL REFERENCES sports(id),
  court_number varchar(255),
  hourly_rate numeric(38,2),
  is_indoor boolean NOT NULL,
  surface_type varchar(255),
  capacity int,
  is_active boolean NOT NULL,
  created_at timestamp
);

CREATE TABLE bookings (
  id uuid PRIMARY KEY,
  user_id uuid NOT NULL REFERENCES users(id),
  court_id uuid NOT NULL REFERENCES courts(id),
  start_time timestamp NOT NULL,
  end_time timestamp NOT NULL,
  status varchar(255) NOT NULL,
  total_price numeric(38,2),
  payment_id varchar(255),
  payment_status varchar(255),
  payment_method varchar(255),
  notes text,
  created_at timestamp,
  confirmed_at timestamp,
  cancelled_at timestamp,
  cancelled_by varchar(255),
  cancellation_reason varchar(255),
  no_show_marked_at timestamp,
  match_id uuid
);
-- As declared on the Booking entity:
CREATE INDEX id_booking_court_time ON bookings (court_id, start_time, end_time);
CREATE INDEX idx_booking_user_time ON bookings (user_id, start_time);

CREATE TABLE court_blocks (
  id uuid PRIMARY KEY,
  court_id uuid NOT NULL REFERENCES courts(id),
  start_time timestamp NOT NULL,
  end_time timestamp NOT NULL,
  reason varchar(200),
  created_at timestamp
);
CREATE INDEX idx_court_block_court_time ON court_blocks (court_id, start_time, end_time);

CREATE TABLE booking_policies (
  id uuid PRIMARY KEY,
  venue_id uuid NOT NULL UNIQUE REFERENCES venues(id),
  slot_increment_minutes int NOT NULL,
  min_booking_minutes int NOT NULL,
  max_booking_minutes int NOT NULL,
  advance_booking_days int NOT NULL,
  min_notice_minutes int NOT NULL,
  cancellation_cutoff_minutes int NOT NULL,
  auto_confirm boolean NOT NULL,
  opening_hours jsonb,
  created_at timestamp,
  updated_at timestamp
);

CREATE TABLE friendships (
  id uuid PRIMARY KEY,
  requester_id uuid NOT NULL REFERENCES users(id),
  receiver_id uuid NOT NULL REFERENCES users(id),
  status varchar(255) NOT NULL,
  created_at timestamp NOT NULL,
  message varchar(500),
  UNIQUE (requester_id, receiver_id)
);

CREATE TABLE notifications (
  id uuid PRIMARY KEY,
  recipient_id uuid NOT NULL REFERENCES users(id),
  sender_id uuid REFERENCES users(id),
  type varchar(255) NOT NULL,
  reference_id uuid,
  message varchar(255) NOT NULL,
  is_read boolean NOT NULL,
  created_at timestamp
);
-- As declared on the Notification entity:
CREATE INDEX idx_notification_recipient_unread ON notifications (recipient_id, is_read);
CREATE INDEX idx_notification_recipient_created ON notifications (recipient_id, created_at);

CREATE TABLE refresh_tokens (
  id uuid PRIMARY KEY,
  user_id uuid NOT NULL,
  token_hash varchar(64) NOT NULL,
  expiry_date timestamptz NOT NULL,
  revoked boolean NOT NULL,
  created_at timestamptz
);
CREATE UNIQUE INDEX idx_refresh_token_hash ON refresh_tokens (token_hash);
CREATE INDEX idx_refresh_user ON refresh_tokens (user_id);

CREATE TABLE password_reset_codes (
  id uuid PRIMARY KEY,
  user_id uuid NOT NULL,
  code_hash varchar(64) NOT NULL,
  expires_at timestamp NOT NULL,
  attempts int NOT NULL,
  used boolean NOT NULL,
  created_at timestamp
);
CREATE INDEX idx_reset_code_user_created ON password_reset_codes (user_id, created_at);

CREATE TABLE matches (
  id uuid PRIMARY KEY,
  booking_id uuid,
  created_by_user_id uuid NOT NULL REFERENCES users(id),
  source varchar(255),
  verification_status varchar(255),
  score varchar(255),
  match_date timestamp,
  is_private boolean NOT NULL,
  status varchar(255),
  ratings_applied boolean NOT NULL,
  created_at timestamp,
  description varchar(255),
  external_opponent_email varchar(255),
  external_opponent_name varchar(255),
  external_verification_token varchar(255)
);

CREATE TABLE participants (
  match_id uuid NOT NULL REFERENCES matches(id),
  user_id uuid NOT NULL REFERENCES users(id),
  status varchar(255),
  is_host boolean NOT NULL,
  team_name varchar(255),
  PRIMARY KEY (match_id, user_id)
);
-- NOTE deliberately ABSENT: any index on participants(user_id). The entity
-- declares none, and the composite PK (match_id, user_id) cannot serve
-- user_id-only predicates. Every MatchRepository "my matches/stats" query
-- filters on user_id — experiments.sql measures the cost and the fix.

CREATE TABLE user_preferences (
  user_id uuid NOT NULL REFERENCES users(id),
  sports_id uuid NOT NULL REFERENCES sports(id),
  skill_level varchar(255),
  is_primary_sport boolean NOT NULL,
  PRIMARY KEY (user_id, sports_id)
);

CREATE TABLE smart_fill_offers (
  id uuid PRIMARY KEY,
  court_id uuid NOT NULL,
  slot_start timestamp NOT NULL,
  user_id uuid NOT NULL,
  sent_at timestamp NOT NULL,
  UNIQUE (court_id, slot_start, user_id)
);
CREATE INDEX idx_offer_court_sent ON smart_fill_offers (court_id, sent_at);
CREATE INDEX idx_offer_user ON smart_fill_offers (user_id);

-- Integer handles for pgbench scripts (pgbench randomizes ints, not uuids).
CREATE TABLE bench_court_ids (idx int PRIMARY KEY, id uuid NOT NULL);
CREATE TABLE bench_user_ids  (idx int PRIMARY KEY, id uuid NOT NULL);
