package web;



import java.io.*;
import java.net.*;

public class FacadeServer {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(35000);
        System.out.println("Servidor corriendo en el puerto 35000");

        while (true) {
            Socket clientSocket = serverSocket.accept();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

                String requestLine = in.readLine();
                if (requestLine == null) continue;

                if (requestLine.startsWith("GET / ")) {
                    enviarHtml(out);
                } else if (requestLine.startsWith("GET /consulta?comando=")) {
                    String comando = requestLine.split("comando=")[1].split(" ")[0];
                    String response = reenviarPeticion(comando);
                    out.println("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\n\r\n" + response);
                } else {
                    out.println("HTTP/1.1 404 Not Found\r\nContent-Type: text/plain\r\n\r\nComando no reconocido.");
                }
            }
        }
    }

    private static void enviarHtml(PrintWriter out) {
        String html = "<!DOCTYPE html>" +
                "<html><head><title>Reflective ChatGPT</title>" +
                "<script>" +
                "function enviarComando() {" +
                " let comando = document.getElementById('comando').value;" +
                " fetch('/consulta?comando=' + encodeURIComponent(comando))" +
                " .then(response => response.text())" +
                " .then(data => document.getElementById('respuesta').innerText = data);" +
                "}" +
                "</script>" +   
                "</head><body>" +
                "<h1>Reflective ChatGPT</h1>" +
                "<input type='text' id='comando' placeholder='Escribe un comando'>" +
                "<button onclick='enviarComando()'>Enviar</button>" +
                "<pre id='respuesta'></pre>" +
                "</body></html>";

        out.println("HTTP/1.1 200 OK\r\nContent-Type: text/html\r\n\r\n" + html);
    }

    private static String reenviarPeticion(String comando) throws IOException {
        String backendUrl = "http://localhost:45000/compreflex?comando=" + comando;
        URL url = new URL(backendUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String response = "", inputLine;
        while ((inputLine = in.readLine()) != null) {
            response += inputLine;
        }
        in.close();
        return response;
    }
}
