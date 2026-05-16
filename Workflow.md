Write VenueServie for distance and the controller, right now datacomes from DB AKA model,
goes to repo layer where it gets fromated in a format we like VenueDistanceProjection and gets 
outputwed as pageable format in VenueRepository, then service layer checks logic is correct or 
not and are we recving the right inputs from the user such as maxDistance etc, adn if correct 
data gets mapped from VenueDistanceProjection format to VenueNearbyResponse DTO and then goes to 
then when we call the APi through controller all this happens
