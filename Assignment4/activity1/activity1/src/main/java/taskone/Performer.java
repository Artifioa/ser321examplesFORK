/**
  File: Performer.java
  Author: Chase Molstad
  Description: Performer class in package taskone.
*/

package taskone;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import netscape.javascript.JSObject;
import org.json.JSONObject;

/**
 * Class: Performer 
 * Description: Performer for server tasks.
 */
class Performer {

    private ArrayList<String> state;
    private Socket conn;

    public Performer(Socket sock, StringList strings) {
        this.conn = sock;
        this.state = strings;
    }

    public JSONObject add(JSONObject req) {
        System.out.println("In add");
        JSONObject resp = new JSONObject();
        resp.put("type", 1);

        boolean missing = false;

        if (!req.has("data")){
            missing = true;
        }
        if(!req.getJSONObject("data").has("string")) {
            missing = true;
        }
        if(missing) {
            resp.put("ok", false);
            resp.put("type", 1);
            JSONObject error = new JSONObject();
            error.put("error", "required data missing");
            error.put("details", "field data or string missing");
            resp.put("data", error);
            return resp;
        }

        String str = req.getJSONObject("data").getString("string");
        state.add(str);
        resp.put("ok", true);
        resp.put("data", state.toString());
        return resp;
    }














    //Display
    public JSONObject display() {
        System.out.println("In display");
        JSONObject resp = new JSONObject();
        resp.put("type", 2);
        resp.put("ok", true);
        resp.put("data", state.toString());
        return resp;
    }

    //Delete All
    public JSONObject deleteAll() {
        System.out.println("In deleteAll");
        state.clear();
        JSONObject resp = new JSONObject();
        resp.put("type", 3);
        resp.put("ok", true);
        resp.put("data", "Successfully deleted the list.");
        return resp;
    }

    //Replace
    public JSONObject replace(JSONObject req) {
        System.out.println("In replace");
        JSONObject resp = new JSONObject();
        resp.put("type", 4);

        boolean missing = false;
        if (!req.has("data")){
            missing = true;
        }
        if(!req.getJSONObject("data").has("index") || !req.getJSONObject("data").has("string")) {
            missing = true;
        }
        if(missing) {
            resp.put("ok", false);
            resp.put("type", 4);
            JSONObject error = new JSONObject();
            error.put("error", "required data missing");
            error.put("details", "field data or index or string missing");
            resp.put("data", error);
            return resp;
        }

        int index = req.getJSONObject("data").getInt("index");
        String str = req.getJSONObject("data").getString("string");

        if (index < 0 || index >= state.size()) {
            resp.put("ok", false);
            resp.put("type", 4);
            JSONObject error = new JSONObject();
            error.put("error", "index out of bounds");
            error.put("details", "Index provided is not in the list.");
            resp.put("data", error);
            return resp;
        }

        state.set(index, str);
        resp.put("ok", true);
        resp.put("data", state.toString());
        return resp;
    }












    public static JSONObject unknown(int type) {
        JSONObject json = new JSONObject();
        json.put("type", type); // echo initial type
        json.put("ok", false);
        JSONObject data = new JSONObject();
        data.put("error", "unknown request");
        json.put("data", data);
        return json;
    }

    public static JSONObject quit() {
        JSONObject json = new JSONObject();
        json.put("type", 0); // echo initial type
        json.put("ok", true);
        json.put("data", "Bye");
        return json;
    }

    public void doPerform() {
        boolean quit = false;
        OutputStream out = null;
        InputStream in = null;
        try {
            out = conn.getOutputStream();
            in = conn.getInputStream();
            System.out.println("Server connected to client:");
            while (!quit) {
                byte[] messageBytes = NetworkUtils.receive(in);
                JSONObject message = JsonUtils.fromByteArray(messageBytes);
                JSONObject returnMessage = new JSONObject();
   
                int choice = message.getInt("selected");
                    switch (choice) {
                        case (1):
                            returnMessage = add(message);
                            break;
                        case (2):
                            returnMessage = display();
                            break;
                        case (3):
                            returnMessage = deleteAll();
                            break;
                        case (4):
                            returnMessage = replace(message);
                            break;
                        case (0):
                            returnMessage = quit();
                            quit = true;
                            break;
                        default:
                            returnMessage = unknown(choice);
                            break;
                    }
                    System.out.println(returnMessage);
                // we are converting the JSON object we have to a byte[]
                byte[] output = JsonUtils.toByteArray(returnMessage);
                NetworkUtils.send(out, output);
            }
            // close the resource
            System.out.println("close the resources of client ");
            out.close();
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
