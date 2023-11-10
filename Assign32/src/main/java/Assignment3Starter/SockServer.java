package Assignment3Starter;

import java.net.*;
import java.io.*;
import org.json.*;
import java.util.Random;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;


/**
 * A class to demonstrate a simple client-server connection using sockets.
 * Ser321 Foundations of Distributed Software Systems
 */
public class SockServer {
    static JSONObject hints;

    static {
        try {
            hints = readHints();
        } catch (FileNotFoundException e) {
            hints = new JSONObject();
            throw new RuntimeException(e);
        }
    }

    public static void main(String args[]) {
        Socket sock;
        try {
            //open socket
            ServerSocket serv = new ServerSocket(8888, 1); // hard coded you should make sure that this is taken from Gradle
            System.out.println("Server ready for connetion");

            String name = "";
            String category = "";
            String imageName = "";
            int hintsShown = 0;
            String image = "";
            // This is just a very simpe start with the project that establishes a basic client server connection and asks for a name
            // You can make any changes you like
            while (true) {
                sock = serv.accept(); // blocking wait
                
                // setup the object reading channel
                ObjectInputStream in = new ObjectInputStream(sock.getInputStream());
                OutputStream out = sock.getOutputStream();
                String s = (String) in.readObject();
                System.out.println("Received message: " + s);
                JSONObject json = new JSONObject(s); // assume message is json
                JSONObject obj = new JSONObject(); // resp object


                if (json.getString("type").equals("start")) { // start request was received and we ask for a name
                    System.out.println("New connection");
                    obj.put("type", "hello");
                    obj.put("value", "Hello, please tell me your name.");
                    obj = sendImg("img/hi.png", obj);
                } 
                else if (json.getString("type").equals("name")) { // name is received
                    System.out.println("- Got a name");
                    name = json.getString("value");
                    obj.put("type", "hello");
                    obj.put("value", "Hello " + name + ", please chose a category animals (a), cities (c), or leader board (l)"); // menu options send
                }
                else if (json.getString("type").equals("category")) { // category is received
                    System.out.println("- Got a category");
                    String categoryValue = json.getString("value").toLowerCase();;
                    if (categoryValue.equals("a")) {
                        category = "animal";
                    } else if (categoryValue.equals("c")) {
                        category = "city";
                    } else if (categoryValue.equals("l")) {
                        System.out.println("- Leaderboard requested");
                        obj.put("type", "leaderboard");
                        obj.put("value", leaderboard(name, 0));
                    } else {
                        obj.put("type", "error");
                        obj.put("value", "unknown category");
                    }
                    if (new Random().nextInt(10) == 0) { // 1 in 10 probability of not showing a new image
                        obj.put("type", "gameover");
                        obj.put("value", "gameover");
                    } else {
                        obj.put("value", "Guess the " + category + " in the image.");
                        image = getRandomImage(category);
                        obj.put("type", "image");
                        obj.put("image", image);
                        obj = sendImg(image, obj);
                        imageName = image.substring(image.lastIndexOf('/') + 1, image.lastIndexOf('.'));
                        System.out.println("Image name: " + imageName);
                    }
                }
                else if (json.getString("type").equals("guess")) { // guess is received
                    System.out.println("- Got a guess");
                    String guess = json.getString("value");
                    System.out.println("Guess: " + guess);
                    System.out.println("Image name: " + imageName);
                    int points = json.optInt("points"); // use optInt to get the value of points field with default value 0
                    System.out.println("Points: " + points);
                    
                    if (guess.equals(imageName)) {
                        System.out.println("Correct! The answer is " + imageName + ". +100 points");

                        obj.put("type", "Guess");
                        obj.put("value", "corect");

                    } else if (guess.contains("hint") || guess.contains("help")) { // hint is received
                        System.out.println("- Got a hint request");
                        imageName = image.substring(image.lastIndexOf('/') + 1, image.lastIndexOf('.'));
                        
                        if (hints.has(imageName)) {
                            JSONArray hintsArray = hints.getJSONArray(imageName);
                        
                            
                    
                            if (hintsShown < 3) {
                                String hint = getRandomHint(imageName, hintsArray);
                    
                                // Deduct points for using a hint
                                points -= 20;
                    
                                // Include updated points in the response
                                obj.put("points", points);
                                obj.put("type", "hint");
                                obj.put("value", hint);
                    
                                // Increment hints shown count
                                hintsShown++;
                        } else {
                            obj.put("type", "hint");
                            obj.put("value", "You have already used all your hints for this image.");
                        }
                    }

                        
                    } else if (guess.contains("done")){
                        System.out.println("Game over");
                        obj.put("type", "win");
                        obj.put("value", "win");
                    }
                    else {
                        obj.put("type", "Guess");
                        obj.put("value", "incorrect");
                    }
                } else { // if the request is not recognized.
                    System.out.println("not sure what you meant");
                    obj.put("type", "error");
                    obj.put("value", "unknown request");
                }
                PrintWriter outWrite = new PrintWriter(sock.getOutputStream(), true);
                outWrite.println(obj.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Method that gets a random filename from a file in a specific directory (e.g. img/animals).
     *
     * @param f which specifies the name of the directory
     * @return string of the image file
     */
    public static String getImage(File f) throws Exception {
        String[] list = f.list();

        Random rand = new Random(); //instance of random class
        int int_random = rand.nextInt(list.length - 1);

        String image = list[int_random + 1];
        System.out.println(image);

        if (image.equals(".DS_Store")) { // since Mac always has that
            image = getImage(f);
        }
        return image;
    }

    /**
     * Method that reads the hint list and returns it as JSONObject
     *
     * @return JSONObject including all the hints for the current game
     */
    public static JSONObject readHints() throws FileNotFoundException {
        FileInputStream in = new FileInputStream("img/hints.txt");
        JSONObject obj = new JSONObject(new JSONTokener(in));
        return obj;
    }

    /**
     * In my implementation this method gets a specific file name, opens it, manipulates it to be send over the network
     * and adds that manipulated image to the given obj which is basically my response to the client. You can do it differently of course
     *
     * @param filename with the image to open
     * @param obj      the current response that the server is creating to be send back to the client
     * @return json object that will be sent back to the client which includes the image
     */

    public static JSONObject sendImg(String filename, JSONObject obj) throws Exception {
        File file = new File(filename);

        if (file.exists()) {
            // read file into byte array
            byte[] fileBytes = Files.readAllBytes(Paths.get(filename));

            // encode byte array using Base64
            String encodedFile = Base64.getEncoder().encodeToString(fileBytes);

            // add encoded file to JSON object
            obj.put("image", encodedFile);
        }
        return obj;
    }

    /**
     * Method that gets a random image from a category.
     *
     * @param category the category to get the image from
     * @return the filename of a random image from the category
     */
    public static String getRandomImage(String category) throws Exception {
        File dir = new File("img/" + category);
        String image = getImage(dir);
        return "img/" + category + "/" + image;
    }
        /**
     * Gets a random hint from the provided hints array.
     *
     * @param imageName The name of the image.
     * @param hintsArray The array of hints for the image.
     * @return A random hint from the hints array.
     */
    private static String getRandomHint(String imageName, JSONArray hintsArray) {
        Random rand = new Random();
        int randomIndex = rand.nextInt(hintsArray.length());
        return hintsArray.getString(randomIndex);
    }

    private static String leaderboard(String name, int points) throws Exception {
        // read leaderboard from file
        File leaderboardFile = new File("leaderboard.json");
        JSONObject leaderboard;
        if (leaderboardFile.exists()) {
            String leaderboardString = new String(Files.readAllBytes(Paths.get("leaderboard.json")));
            leaderboard = new JSONObject(leaderboardString);
        } else {
            leaderboard = new JSONObject();
        }

        // update leaderboard with new player's score
        String playerName = name; // replace with actual player name
        int playerScore = points; // replace with actual player score
        if (leaderboard.has(playerName)) {
            int currentScore = leaderboard.getInt(playerName);
            if (playerScore > currentScore) {
                leaderboard.put(playerName, playerScore);
            }
        } else {
            leaderboard.put(playerName, playerScore);
        }

        // write updated leaderboard to file
        Files.write(Paths.get("leaderboard.json"), leaderboard.toString().getBytes());

        // format leaderboard as string and return
        StringBuilder sb = new StringBuilder();
        sb.append("Leaderboard:\n");
        for (String names : leaderboard.keySet()) {
            sb.append(names).append(": ").append(leaderboard.getInt(names)).append("\n");
        }
        return sb.toString();
    }
}
