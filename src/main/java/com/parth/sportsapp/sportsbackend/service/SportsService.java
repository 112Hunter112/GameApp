package com.parth.sportsapp.sportsbackend.service;

import com.parth.sportsapp.sportsbackend.model.Sports;
import com.parth.sportsapp.sportsbackend.repository.SportsRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@Transactional
public class SportsService {

  @Autowired
  private SportsRepository sportsRepository;

  private static final int MAX_PLAYERS_LIMIT = 100;
  private static final int MAX_DESCRIPTION_LENGTH = 500;
  private static final int FUZZY_SEARCH_MIN_LENGTH = 3;
  private static final int FUZZY_SEARCH_MAX_DISTANCE = 2;


  // Regex to find URLs (http/https/www)
  private static final Pattern URL_PATTERN = Pattern.compile("(?i)\\b((?:https?://|www\\d{0,3}[.]|[a-z0-9.\\-]+[.][a-z]{2,4}/)(?:[^\\s()<>]+|\\((?:[^\\s()<>]+|\\([^\\s()<>]+\\))*\\))+(?:\\((?:[^\\s()<>]+|\\([^\\s()<>]+\\))*\\)|[^\\s`!()\\[\\]{};:'\".,<>?«»“”‘’]))");

  // Regex for valid image extensions
  private static final Pattern IMAGE_EXTENSION_PATTERN = Pattern.compile("([^\\s]+(\\.(?i)(jpg|jpeg|png|webp|svg))$)");


  public class InvalidSportDataException extends RuntimeException {
    public InvalidSportDataException(String message) {
      super(message);
    }
  }



  public List<Sports> getAllActiveSports() {
    // For Vendors and Users: Only show what they can actually play
    return sportsRepository.findByIsActiveTrue();
  }

  public List<Sports> getAllSportsForAdmin() {
    // For You (Admin): Show everything, even deleted ones
    return sportsRepository.findAll();
  }

  // loads all the info fo the sport via ID
  public Sports getSportById(UUID id) {
    return sportsRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Sport not found with ID: " + id));
  }

  /**
   * Searches for the sports name by how the Admin has typed the name
   * @param keyword the input by the admin trying to find the name
   * @return list of all possible sports
   */
  public List<Sports> searchSports(String keyword) {
    // This calls the repository method you made earlier
    // It will find "Soccer", "Social Tennis", "Socca"
    return sportsRepository.findBySportNameContainingIgnoreCase(keyword);
  }




  // verify if the sport being added is correct
  public void sportInputCheck(int min_player, int max_player, String sport_name,
      String description, String url) {

    if (min_player > max_player) {
      throw new RuntimeException("Minimum players cannot be greater than maximum players.");
    }

    // 2. Hardcoded Safety Limit
    if (max_player > MAX_PLAYERS_LIMIT) {
      throw new RuntimeException("Max players cannot exceed 100 (Safety Limit).");
    }

    //3. Check if description is malicious and max length is 500
    if (description != null) {
      if (description.length() > MAX_DESCRIPTION_LENGTH) {
        throw new RuntimeException("Description cannot exceed 500 characters.");
      }
      if (URL_PATTERN.matcher(description).find()) {
        throw new RuntimeException("Description cannot contain URLs for security reasons.");
      }
    }
    
    // 4. Icon URL Validation (Extension Check)
    validateIconURL(url);

  }

  private void validateIconURL(String url) {
    if (url == null || url.isBlank()) {
      return; // Optional field
    }

    // Check if it's a valid URL format
    if (!url.startsWith("http://") && !url.startsWith("https://")) {
      throw new InvalidSportDataException("Icon URL must start with http:// or https://");
    }

    // Check file extension
    if (!IMAGE_EXTENSION_PATTERN.matcher(url).matches()) {
      throw new InvalidSportDataException("Icon URL must end in .jpg, .png, .webp, or .svg");
    }

    // Optional: Check URL length
    if (url.length() > 500) {
      throw new InvalidSportDataException("Icon URL is too long (max 500 characters)");
    }
  }



  // --  Verify Admin Input
  public Sports addSport(Sports sport) {

    sportInputCheck(sport.getMinPlayers(), sport.getMaxPlayers(), sport.getSportName(),
        sport.getDescription(), sport.getIconURL());

// chekc for duplicates ignore upper lower cases
    if(sportsRepository.existsBySportNameIgnoreCase(sport.getSportName())) {
      throw new RuntimeException("Sport name already exists");
    }

    checkForSimilarNames(sport.getSportName());


    return sportsRepository.save(sport);
  }




  // -- Verify Admin update to sport

  public Sports updateSport(UUID oldSport_UUID,Sports newSport) {

    Sports sport = sportsRepository.findById(oldSport_UUID)
        .orElseThrow(() -> new RuntimeException("Sport not found"));

    Sports existingSport = getSportById(oldSport_UUID);

    // validate the new information of the sport
    sportInputCheck(newSport.getMinPlayers(), newSport.getMaxPlayers(), newSport.getSportName(),
        newSport.getDescription(), newSport.getIconURL());

    if (!existingSport.getSportName().equalsIgnoreCase(newSport.getSportName()) &&
        sportsRepository.existsBySportNameIgnoreCase(newSport.getSportName())) {
      throw new RuntimeException("Sport name already taken!");
    }

    existingSport.setSportName(newSport.getSportName());
    existingSport.setDescription(newSport.getDescription());
    existingSport.setIconURL(newSport.getIconURL());
    existingSport.setMinPlayers(newSport.getMinPlayers());
    existingSport.setMaxPlayers(newSport.getMaxPlayers());


    return sportsRepository.save(existingSport);
  }



  // -- toggle for Active status
  public void toggleSportStatus(UUID id) {
    Sports sport = getSportById(id);
    // Flip the switch (True -> False, False -> True)
    sport.setActive(!sport.isActive());
    sportsRepository.save(sport);
  }

  // A simple "Fuzzy Search" helper
  private void checkForSimilarNames(String newName) {
    List<Sports> activeSports = sportsRepository.findByIsActiveTrue();

    for (Sports existing : activeSports) {
      // Calculate "Levenshtein Distance" (how many edits to turn string A into B)
      // If the difference is very small (e.g., 1 character), it's a likely typo
      int distance = calculateLevenshteinDistance(newName.toLowerCase(), existing.getSportName().toLowerCase());

      // If names are nearly identical (distance <= 2) and length is similar
      if (distance <= FUZZY_SEARCH_MAX_DISTANCE && newName.length() > FUZZY_SEARCH_MIN_LENGTH) {
        throw new RuntimeException("This name is too similar to existing sport: " + existing.getSportName() + ". Did you make a typo?");
      }
    }
  }

  // Standard Algorithm for string similarity
  private int calculateLevenshteinDistance(String x, String y) {
    int[][] dp = new int[x.length() + 1][y.length() + 1];
    for (int i = 0; i <= x.length(); i++) dp[i][0] = i;
    for (int j = 0; j <= y.length(); j++) dp[0][j] = j;
    for (int i = 1; i <= x.length(); i++) {
      for (int j = 1; j <= y.length(); j++) {
        int cost = (x.charAt(i - 1) == y.charAt(j - 1)) ? 0 : 1;
        dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
      }
    }
    return dp[x.length()][y.length()];
  }
}
