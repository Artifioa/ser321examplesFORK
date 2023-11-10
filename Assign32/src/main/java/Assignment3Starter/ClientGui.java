package Assignment3Starter;

import java.awt.Dimension;

import org.json.*;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.swing.JDialog;
import javax.swing.WindowConstants;
import javax.swing.JOptionPane;
import javax.swing.ImageIcon;
import java.util.Base64;
import javax.swing.JLabel;

/**
 * The ClientGui class is a GUI frontend that displays an image grid, an input text box,
 * a button, and a text area for status. 
 * 
 * Methods of Interest
 * ----------------------
 * show(boolean modal) - Shows the GUI frame with the current state
 *     -> modal means that it opens the GUI and suspends background processes. Processing 
 *        still happens in the GUI. If it is desired to continue processing in the 
 *        background, set modal to false.
 * newGame(int dimension) - Start a new game with a grid of dimension x dimension size
 * insertImage(String filename, int row, int col) - Inserts an image into the grid
 * appendOutput(String message) - Appends text to the output panel
 * submitClicked() - Button handler for the submit button in the output panel
 * 
 * Notes
 * -----------
 * > Does not show when created. show() must be called to show he GUI.
 * 
 */
public class ClientGui implements Assignment3Starter.OutputPanel.EventHandlers {
	JDialog frame;
	PicturePanel picturePanel;
	OutputPanel outputPanel;
	String currentMessage;
	Socket sock;
	OutputStream out;
	ObjectOutputStream os;
	BufferedReader bufferedReader;
	boolean nameSent = false;
	boolean categorySent = false;
	String host = "localhost";
	int port = 9000;
	int points = 0;

	/**
	 * Construct dialog
	 * @throws IOException 
	 */
	public ClientGui(String host, int port, String id) throws IOException {
		this.host = host;
		this.port = port;

		// ---- GUI things you do not have to change/touch them ----
		frame = new JDialog();
		frame.setLayout(new GridBagLayout());
		frame.setMinimumSize(new Dimension(500, 500));
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);


		// setup the top picture frame
		picturePanel = new PicturePanel();
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weighty = 0.25;
		frame.add(picturePanel, c);

		// setup the input, button, and output area
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 1;
		c.weighty = 0.75;
		c.weightx = 1;
		c.fill = GridBagConstraints.BOTH;
		outputPanel = new OutputPanel();
		outputPanel.addEventHandlers(this);
		frame.add(outputPanel, c);

		picturePanel.newGame(1);

		// ---- GUI things end ----

		open(); // open connection to server
		currentMessage = "{'type': 'start'}"; // sending a start request to the server
		try {
			os.writeObject(currentMessage); // send to server
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		String string = this.bufferedReader.readLine(); // wait for answer
		JSONObject json = new JSONObject(string); // assumes answer is a JSON
		outputPanel.appendOutput(json.getString("value")); // write output value to output panel

		try {
			picturePanel.insertImage("img/hi.png", 0, 0); // hard coded to open this image -- image (not path) should be read from server message
		} catch (Exception e){
			System.out.println(e);
		}
		close(); // close connection to server
	}

	/**
	 * Shows the current state in the GUI
	 * @param makeModal - true to make a modal window, false disables modal behavior
	 */
	public void show(boolean makeModal) {
		frame.pack();
		frame.setModal(makeModal);
		frame.setVisible(true);
	}

	/**
	 * Submit button handling
	 * 
	 * Change this to whatever you need, this is where the action happens. Tip outsource things to methods though so this method
	 * does not get too long
	 */
	@Override
	public void submitClicked() {
		try {
			open();
			System.out.println("submit clicked"); // server connection opened
			String input = outputPanel.getInputText(); // Pulls the input box text
			System.out.println(nameSent);
			System.out.println(categorySent);

			if (input.length() > 0 && nameSent == false) {
				outputPanel.appendOutput(input); // append input to the output panel
				outputPanel.setInputText(""); // clear input text box

				// Send name to the server only if it hasn't been sent before
				JSONObject nameObj = new JSONObject();
				nameObj.put("type", "name");
				nameObj.put("value", input);
				try {
					os.writeObject(nameObj.toString()); // sending the name to the server
				} catch (IOException e) {
					e.printStackTrace();
				}

				// Print the sent message
				System.out.println("Sent message: " + nameObj.toString());

				nameSent = true; // Set the flag to true after sending the name
				outputPanel.appendOutput("Choose a category (a for animals, c for cities, l for leader board)");
				outputPanel.setPoints(0); // You might want to set appropriate points
				outputPanel.setInputText(""); // Clear input text box
			}

			// Prompt the user for the category and send it to the server
			else if (input.length() > 0 && nameSent == true && categorySent == false){
			
				// reset points to 0
				// Wait until the user submits a category
				while (input.isEmpty()) {
					try {
						Thread.sleep(100); // wait for 100 milliseconds
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					input = outputPanel.getInputText();
				}

				// Assuming the user's choice is in the input text box
				String category = input.toLowerCase(); // Convert to lowercase for consistency

				// Create a new JSON object for the category and send it to the server
				JSONObject categoryObj = new JSONObject();
				categoryObj.put("type", "category");
				categoryObj.put("value", category);

				try {
					os.writeObject(categoryObj.toString()); // sending the category to the server
				} catch (IOException e) {
					e.printStackTrace();
				}

				// Print the sent message
				System.out.println("Sent message: " + categoryObj.toString());
				categorySent = true; // Set the flag to true after sending the category
				outputPanel.setTask("Guess the word");
				outputPanel.setInputText(""); // Clear input text box
				// Receive the response from the server
				String response = "";
				
				
				try {
					response = bufferedReader.readLine();
					System.out.println(response);
				} catch (IOException e) {
					e.printStackTrace();
				}

				// Parse the JSON string received from the server
				JSONObject json = new JSONObject(response);
				if (json.getString("type").equals("leaderboard")) {
						JSONArray leaderboard = json.getJSONArray("leaderboard");
						System.out.println(leaderboard.toString());
				}
				// BEGIN
				String encodedImage = json.getString("image");
				byte[] decodedBytes = Base64.getDecoder().decode(encodedImage);
				ByteArrayInputStream imageStream = new ByteArrayInputStream(decodedBytes);

				// Assuming picturePanel.insertImage accepts ByteArrayInputStream
				try {
					// Assuming picturePanel.insertImage accepts ByteArrayInputStream
					picturePanel.insertImage(imageStream, 0, 0);
				} catch (PicturePanel.InvalidCoordinateException e) {
					// Handle the exception, e.g., print an error message
					e.printStackTrace();
				}

				// END

			}

			else if (input.length() > 0 && nameSent == true && categorySent == true){
			// Send hints to the server
					String guess = input.toLowerCase(); // Assuming the user's choice is in the input text box
					JSONObject guessObj = new JSONObject();
					guessObj.put("type", "guess");
					guessObj.put("value", guess);

					try {
						os.writeObject(guessObj.toString()); // sending the guess to the server
					} catch (IOException e) {
						e.printStackTrace();
					}

					// Print the sent message
					System.out.println("Sent message: " + guessObj.toString());

					// Receive the response from the server
					String response = "";
					try {
						response = bufferedReader.readLine();
						System.out.println(response);
					} catch (IOException e) {
						e.printStackTrace();
					}
					// Check if the user has guessed the word correctly
					
					if (response.contains("corect")) {
						points += 100; // increment points by 100
						outputPanel.setTask("You guessed the word correctly! You earned 100 points.");
						outputPanel.appendOutput("You now have " + points + " points.");
						outputPanel.setPoints(points);
						outputPanel.setInputText(""); // Clear input text box
						categorySent = false; // reset categorySent flag
						outputPanel.appendOutput("Choose a category (a for animals, c for cities, l for leader board)");
					}
					else if (response.contains("incorrect")) {
						points -= 30; // decrement points by 30
						outputPanel.setTask("You guessed the word incorrectly! You lost 30 points.");
						outputPanel.appendOutput("You now have " + points + " points.");
						outputPanel.appendOutput(guess); // append guess to the output panel
						outputPanel.setInputText(""); // clear input text box
						outputPanel.setPoints(points);
					}
					else if (response.contains("hint")){
						points -= 20; // decrement points by 20
						outputPanel.setTask("You requested a hint! You lost 20 points.");
						outputPanel.appendOutput("You now have " + points + " points.");
						JSONObject hintObj = new JSONObject(response);
						String hint = hintObj.getString("value");
						outputPanel.appendOutput(hint); // append hint to the output panel
						outputPanel.setInputText(""); // clear input text box
						outputPanel.setPoints(points);
					}
					else if (response.contains("win")) {
						try {
							picturePanel.insertImage("img/win.png", 0, 0);/* 
							picturePanel.repaint();
							picturePanel.revalidate();
							picturePanel.revalidate();
							picturePanel.repaint();
							picturePanel.insertImage("img/win.png", 0, 0);*/

							outputPanel.setTask("Game over");
							outputPanel.appendOutput("You finished with " + points + " points.");
							outputPanel.setInputText(""); // clear input text box
							outputPanel.setPoints(0);
							nameSent = false; // reset nameSent flag
							categorySent = false; // reset categorySent flag
							outputPanel.appendOutput("Input your name to keep playing");
						} catch (Exception e) {
							System.out.println("Error displaying win image: " + e.getMessage());
						}
					}
					
					else if (response.contains("gameover")){
						try {
							picturePanel.insertImage("img/lose.png", 0, 0); // hard coded to open this image -- image (not path) should be read from server message
						} catch (Exception e){
							System.out.println(e);
						}
						outputPanel.setTask("Random game over");
						outputPanel.appendOutput("You lost with " + points + " points.");
						outputPanel.setInputText(""); // clear input text box
						outputPanel.setPoints(0);
						nameSent = false; // reset nameSent flag
						categorySent = false; // reset categorySent flag
						outputPanel.appendOutput("Input your name to keep playing");
					}
					
				}
			
			 
			close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	


	@Override
	public void allInClicked(){
		String input = outputPanel.getInputText();
		String guess = input.toLowerCase(); // Assuming the user's choice is in the input text box
		JSONObject guessObj = new JSONObject();
		guessObj.put("type", "guess");
		guessObj.put("value", guess);
		if (input.isEmpty()) {
			// If the input is empty, show an error message and return
			JOptionPane.showMessageDialog(frame, "Please enter your answer before clicking All In.");
			return;
		}
		try {
			open();
			os.writeObject(guessObj.toString()); // sending the guess to the server

			// Receive the response from the server
			String response = "";
			try {
				response = bufferedReader.readLine();
				System.out.println(response);
			} catch (IOException e) {
				e.printStackTrace();
			}
			// Check if the user has guessed the word correctly
			if (response.contains("corect")) {
				points += 200; // increment points by 100
				outputPanel.setTask("Your All in succeeded! You earned 200 points.");
				outputPanel.setPoints(points);
				outputPanel.setInputText(""); // Clear input text box
				categorySent = false; // reset categorySent flag
				outputPanel.appendOutput("Choose a category (a for animals, c for cities, l for leader board)");
			}
			else if (response.contains("incorrect")) {
				points -= 200; // decrement points by 30
				outputPanel.setTask("Your All in failed! You lost 200 points.");
				outputPanel.setInputText(""); // clear input text box
				outputPanel.setPoints(points);
				categorySent = false; // reset categorySent flag
				outputPanel.appendOutput("Choose a category (a for animals, c for cities, l for leader board)");

			}
		} catch (IOException e) {
						e.printStackTrace();
					}
		 finally {
			close();
		}
	}

	public void open() throws UnknownHostException, IOException {
		this.sock = new Socket(host, port); // connect to host and socket on port 8888

		// get output channel
		this.out = sock.getOutputStream();
		// create an object output writer (Java only)
		this.os = new ObjectOutputStream(out);
		this.bufferedReader = new BufferedReader(new InputStreamReader(sock.getInputStream()));

	}
	
	public void close() {
		try {
			if (out != null)  out.close();
			if (bufferedReader != null)   bufferedReader.close(); 
			if (sock != null) sock.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws IOException {
		// create the frame

		try { // hard coded you should make sure that this is taken from Gradle
			String host = "localhost";
			int port = 8888;


			ClientGui main = new ClientGui(host, port, args[0]);
			main.show(true);


		} catch (Exception e) {e.printStackTrace();}



	}
}
