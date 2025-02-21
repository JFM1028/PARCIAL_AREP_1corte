package web;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;

public class HttpServer {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(45000);
        System.out.println("Servidor backend corriendo en el puerto 45000...");

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
                    String response = procesarPeticion(comando);
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

    private static String procesarPeticion(String comando) {
        try {
            if (comando.startsWith("Class")) {
                return obtenerInfoClase(comando);
            } else if (comando.startsWith("invoke")) {
                return ejecutarMetodo(comando);
            } else if (comando.startsWith("unaryInvoke")) {
                return ejecutarMetodoUnario(comando);
            } else if (comando.startsWith("binaryInvoke")) {
                return ejecutarMetodoBinario(comando);
            } else {
                return "{\"error\": \"Comando no reconocido\"}";
            }
        } catch (Exception e) {
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    private static String obtenerInfoClase(String comando) throws ClassNotFoundException {
        String className = comando.substring(6, comando.length() - 1);
        Class<?> clazz = Class.forName(className);

        String json = "{";
        json += "\"className\":\"" + clazz.getName() + "\",";
        json += "\"fields\":[";

        Field[] fields = clazz.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            json += "\"" + fields[i].toString() + "\"";
            if (i < fields.length - 1) json += ",";
        }
        json += "],";

        json += "\"methods\":[";
        Method[] methods = clazz.getDeclaredMethods();
        for (int i = 0; i < methods.length; i++) {
            json += "\"" + methods[i].toString() + "\"";
            if (i < methods.length - 1) json += ",";
        }
        json += "]}";

        return json;
    }

    private static String ejecutarMetodo(String comando) throws Exception {
        String[] partes = comando.substring(7, comando.length() - 1).split(",");
        Class<?> clazz = Class.forName(partes[0]);
        Method method = clazz.getMethod(partes[1]);
        Object resultado = method.invoke(null);
        return "{\"result\":\"" + resultado + "\"}";
    }

    private static String ejecutarMetodoUnario(String comando) throws Exception {
        String[] partes = comando.substring(12, comando.length() - 1).split(",");
        Class<?> clazz = Class.forName(partes[0]);
        Method method = clazz.getMethod(partes[1], getTipo(partes[2]));
        Object resultado = method.invoke(null, convertirValor(partes[2], partes[3]));
        return "{\"result\":\"" + resultado + "\"}";
    }

    private static String ejecutarMetodoBinario(String comando) throws Exception {
        String[] partes = comando.substring(13, comando.length() - 1).split(",");
        Class<?> clazz = Class.forName(partes[0]);
        Method method = clazz.getMethod(partes[1], getTipo(partes[2]), getTipo(partes[4]));
        Object resultado = method.invoke(null, convertirValor(partes[2], partes[3]), convertirValor(partes[4], partes[5]));
        return "{\"result\":\"" + resultado + "\"}";
    }

    private static Class<?> getTipo(String tipo) {
        if (tipo.equals("int")) return int.class;
        if (tipo.equals("double")) return double.class;
        if (tipo.equals("String")) return String.class;
        throw new IllegalArgumentException("Tipo no soportado: " + tipo);
    }

    private static Object convertirValor(String tipo, String valor) {
        if (tipo.equals("int")) return Integer.parseInt(valor);
        if (tipo.equals("double")) return Double.parseDouble(valor);
        if (tipo.equals("String")) return valor;
        throw new IllegalArgumentException("Tipo no soportado: " + tipo);
    }
}

