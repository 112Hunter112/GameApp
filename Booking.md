We need ot conider booking needs websockets to handel instant updates to number of people 
jopiming or leaving a game.

* Missing: Two-Way Review & Rating System
    * Concept: You have EloRating (skill), but no Reliability rating. Did the player actually show up? Were they toxic? Are the
      venue's bathrooms clean? Is the court surface torn up?
    * Code Needs: A Review entity. Players review Venues. Players review Players. You also need a "Flake Score" / No-Show Counter on
      the User model to ban people who join lobbies but don't show up.
* Missing: Split Payments & Escrow
    * Concept: Right now, Booking has one totalPrice. If 10 people play, does the host pay $100 and hope 9 strangers Venmo him? No,
      the app must handle it. When a user clicks "Join Match", it authorizes a $10 hold on their card. If the match is cancelled, the
      hold drops.
    * Code Needs: A ParticipantPayment entity to track individual charges linked to a specific Participants row.
* Missing: Waitlists
    * Concept: If prime-time Friday 7 PM courts are booked, users should be able to join a waitlist. If a booking is cancelled, the
      system automatically texts the next person in line.
    * Code Needs: A Waitlist entity (User, Court/Venue, TimeSlot).
* Missing: Dynamic Pricing & Cancellation Policies
    * Concept: Tuesday at 10 AM should be cheaper than Saturday at 6 PM. Venues need cancellation rules (e.g., "Non-refundable within
      24 hours").
    * Code Needs: A PricingRule entity attached to Courts, and a CancellationPolicy entity.


# **Booking Logic**

1. Step 1 (Selection): The user clicks "11:00 AM - 12:00 PM". (This does not create a booking yet).
2. Step 2 (Checkout): They go to a checkout screen, review the price, select their player limit for the lobby, enter payment info, and
   click "Confirm & Pay"

We will have semi-open time scheduled eg. [10,11) so [11,12) wont throw a logic erro when we use it.






