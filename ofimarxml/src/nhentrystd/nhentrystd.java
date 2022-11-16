package nhentrystd;

import engiutils.AppAliasData;
import engiutils.ELimiter;
import engiutils.ELimiterToken;
import engiutils.EServlet;
import engiutils.EServletConfig;
import engiutils.EServletKernel;
import engiutils.EStringUtils;
import engiutils.ErrorContextBuilder;
import engiutils.GETStatusServlet;
import engiutils.SXMLClient;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.xml.rpc.holders.IntHolder;

public class nhentrystd extends HttpServlet implements EServlet {
   private static final long serialVersionUID = 2745874059239740522L;
   private static final String SERVLET_NAME = "eitxml";
   private static final String CONTENT_TYPE = "text/xml";
   private static final String CONTENT_TYPE_GET = "text/html";
   private static final String HEADER_VAR = "EITAPP";
   private static final String HEADER_SIG = "eit-Server-Signature";
   private static final String STATUS_OK = "OK";
   private static final String STATUS_ERR = "ERROR";
   private static final String VERSION = "2.7";
   private static final String VER_FECHA = "02-03-2011";
   private static final String CONFIG_PATH = "/../eitech/config/eitxml.cfg";
   private ELimiter lim;
   private int maxCon;
   private int limTimeout;
   protected EServletKernel kernel;
   private boolean isEitechV4;

   public void init() throws ServletException {
      this.kernel = new EServletKernel(this, "/../eitech/config/eitxml.cfg", "engisoft_eit", "ErrorText", "eitxml");
      EServletConfig lc = this.kernel.getServletConfig();
      this.maxCon = lc.maxcon;
      this.limTimeout = lc.timeout;
      this.isEitechV4 = lc.eitechv4;
      this.lim = new ELimiter(this.maxCon);
   }

   public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
      response.setContentType("text/html");
      PrintWriter out = response.getWriter();

      try {
         this.getWork(request, out);
      } catch (Throwable var5) {
         out.println("<html>");
         out.println("<head>");
         out.println("<meta content=\"text/html; charset=ISO-8859-1\"");
         out.println("http-equiv=\"content-type\">");
         out.println("<title>Test de servlet</title>");
         out.println("</head>");
         out.println("<body style=\"color: rgb(0, 0, 0); background-color: rgb(255, 255, 255);\"");
         out.println("alink=\"#000088\" link=\"#0000ff\" vlink=\"#ff0000\">");
         out.println("Status: Se ha producido una excepcion (" + var5.getMessage() + ")<br>");
         out.println("<br>");
         out.println("</body>");
         out.println("</html>");
      }

      ErrorContextBuilder.dump();
   }

   private void getWork(HttpServletRequest request, PrintWriter out) throws SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
      if (request.getParameter("TestConnectionId") == null && request.getParameter("version") == null) {
         this.kernel.doConfigWork(request, out);
      } else {
         this.doTestWork(request, out);
      }

   }

   private void doTestWork(HttpServletRequest request, PrintWriter out) {
      String ID = request.getParameter("TestConnectionId");
      if (ID == null) {
         String V = request.getParameter("version");
         if (V == null) {
            this.kernel.printEmuleGetError(out);
         } else {
            this.printVersion(out);
         }
      } else if (ID.equals("2")) {
         this.kernel.printStatus(out, "OK");
      } else if (this.DoTestConnection(ID) == 0) {
         this.kernel.printStatus(out, "OK");
      } else {
         this.kernel.printStatus(out, "ERROR");
      }

   }

   private void printVersion(PrintWriter out) {
      out.println("<html>");
      out.println("<head>");
      out.println("<meta content=\"text/html; charset=ISO-8859-1\"");
      out.println("http-equiv=\"content-type\">");
      out.println("<title>Versi�n del servlet</title>");
      out.println("</head>");
      out.println("<body style=\"color: rgb(0, 0, 0); background-color: rgb(255, 255, 255);\"");
      out.println("alink=\"#000088\" link=\"#0000ff\" vlink=\"#ff0000\">");
      out.println("eitxml 2.7 (02-03-2011)<br>");
      out.println("<br>");
      out.println("</body>");
      out.println("</html>");
   }

   private int DoTestConnection(String ID) {
      SXMLClient c = new SXMLClient();
      AppAliasData ad = this.kernel.getAppData();
      int st = ad.getAliasConfig((String)null);
      if (st != 0) {
         return 1;
      } else {
         try {
            st = c.login(ad.ip, ad.port, ad.appName, ad.fdsApp, ad.loadLimitMode);
            if (st != 0) {
               return 1;
            }
         } catch (Throwable var8) {
            return 1;
         }

         String result;
         try {
            result = c.execFunc(this.getTestSendDoc(ID));
            if (result != null && !result.equals("")) {
               st = 0;
            } else {
               st = 1;
            }
         } catch (Throwable var9) {
            result = "";
            st = 1;
         }

         try {
            c.logout();
         } catch (Throwable var7) {
            st = 1;
         }

         if (st == 0) {
            st = this.GetResultInXML(result);
         }

         return st;
      }
   }

   private String getTestSendDoc(String ID) {
      String result = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
      result = result + "<TestConnection id=\"" + ID + "\"/>";
      return result;
   }

   private int GetResultInXML(String xml) {
      if (xml.indexOf("<TestConnectionResult ") == -1) {
         return 1;
      } else {
         int posI = xml.indexOf("id=");
         if (posI == -1) {
            return 1;
         } else {
            int posF = xml.indexOf("\"/>", posI);
            if (posF == -1) {
               return 1;
            } else {
               String id = xml.substring(posI + 4, posF);

               int st;
               try {
                  st = Integer.parseInt(id);
               } catch (NumberFormatException var7) {
                  st = 1;
               }

               return st;
            }
         }
      }
   }

   public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
      response.setContentType("text/xml");
      if (this.isEitechV4) {
         this.kernel.setCORS(request, response);
      }

      try {
         String hv = this.getServletContext().getServerInfo() + " at " + this.kernel.getHostAddress() + " Port " + request.getServerPort();
         response.addHeader("eit-Server-Signature", hv);
         String userAgent = request.getHeader("engiAgent");
         if (userAgent != null) {
            this.execAdmin(request.getInputStream(), response.getOutputStream(), request.getHeader("EITAPP"), request.getRemoteAddr());
         } else {
            this.execNhEntry(request.getInputStream(), response.getOutputStream(), request.getHeader("EITAPP"), request.getRemoteAddr());
         }
      } catch (Throwable var5) {
         this.PrintExceptionHeader(var5);
         var5.printStackTrace();
      }

      response.getOutputStream().close();
      ErrorContextBuilder.dump();
   }

   protected void doOptions(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
      this.kernel.setCORS(request, response);
      response.getOutputStream().write("".getBytes());
      super.doOptions(request, response);
   }

   private void execAdmin(ServletInputStream is, ServletOutputStream os, String alias, String clientIP) throws IOException {
      ByteArrayOutputStream docIn;
      try {
         docIn = EStringUtils.streamToArray(is);
      } catch (Throwable var8) {
         this.PrintExceptionHeader(var8);
         System.out.println("Error. No se ha recibido el documento de peticion. (nhentrystd.execNHEntry).");
         var8.printStackTrace();
         String sErr = "<engisoft_eit><proc_result type=\"error\"><ErrorText>Error. No se ha recibido el documento de peticion.";
         sErr = sErr + "</ErrorText></proc_result></engisoft_eit>";
         os.write(sErr.getBytes());
         return;
      }

      String sOut = this.checkActionRequired(docIn.toString());
      os.write(sOut.getBytes());
   }

   public void execNhEntry(InputStream is, OutputStream os, String alias, String clientIP) throws IOException {
      ByteArrayOutputStream docIn;
      try {
         docIn = EStringUtils.streamToArray(is);
      } catch (Throwable var14) {
         this.PrintExceptionHeader(var14);
         System.out.println("Error. No se ha recibido el documento de peticion. (nhentrystd.execNHEntry).");
         var14.printStackTrace();
         String sErr = "<engisoft_eit><proc_result type=\"error\"><ErrorText>Error. No se ha recibido el documento de peticion.";
         sErr = sErr + "</ErrorText></proc_result></engisoft_eit>";
         os.write(sErr.getBytes());
         return;
      }

      String sOut;
      if (this.kernel.isWebmoduleAccessible()) {
         ELimiterToken stLim = this.lim.waitLimiter(this.limTimeout * 1000);
         switch(stLim.getLimiterStatus()) {
         case 1:
            ByteArrayOutputStream docOut;
            try {
               docOut = this.doWork(docIn, alias, clientIP);
            } finally {
               this.lim.releaseLimiter(stLim);
            }

            docOut.writeTo(os);
            break;
         case 2:
         default:
            sOut = "<engisoft_eit><proc_result type=\"error\">";
            sOut = sOut + "<ErrorText>Servlet request timed out</ErrorText>";
            sOut = sOut + "</proc_result></engisoft_eit>";
            os.write(sOut.getBytes());
            break;
         case 3:
            sOut = "<engisoft_eit><proc_result type=\"error\" changepage=\"outofservice.html\">";
            sOut = sOut + "</proc_result></engisoft_eit>";
            os.write(sOut.getBytes());
         }
      } else {
         sOut = this.checkActiveServlets((String)null);
         os.write(sOut.getBytes());
      }

   }

   private String checkActionRequired(String doc) {
      String result = "";
      result = this.checkOpenCloseServlets(doc);
      if (result.length() <= 0) {
//         result = this.checkStatusInfo(doc);
         if (result.length() <= 0) {
//            result = this.checkSetConfig(doc);
            if (result.length() <= 0) {
               result = this.checkActiveServlets(doc);
               if (result.length() > 0) {
               }
            }
         }
      }

      return result;
   }

   private String checkActiveServlets(String doc) {
      String result = "";
      if (!this.kernel.isWebmoduleAccessible()) {
         result = "<engisoft_eit><proc_result type=\"error\" changepage=\"outofservice.html\">";
         result = result + "</proc_result></engisoft_eit>";
      }

      return result;
   }

   private String checkOpenCloseServlets(String doc) {
      String result;
      if (this.docIsCloseServletAccess(doc)) {
         result = "<engisoft_eit><proc_result type=\"error\"><ErrorText>Configuracion de acceso actualizada.\n";
         result = result + "Active=" + this.kernel.isWebmoduleAccessible();
         result = result + "</ErrorText></proc_result></engisoft_eit>";
      } else {
         result = "";
      }

      return result;
   }

   private String checkStatusInfo(String doc) {
      return POSStatusServlet.getXML(doc, this.maxCon, this.limTimeout, this.lim);
   }

   private String checkSetConfig(String doc) {
      IntHolder ihMaxCon = new IntHolder(this.maxCon);
      IntHolder ihTimeOut = new IntHolder(this.limTimeout);
      String result = POSConfigServlet.getXML(doc, ihMaxCon, ihTimeOut);
      this.maxCon = ihMaxCon.value;
      this.lim.setMaxValue(this.maxCon);
      this.limTimeout = ihTimeOut.value;
      return result;
   }

   private boolean docIsCloseServletAccess(String doc) {
      int si = doc.indexOf("<CloseServletAccess>");
      if (si == -1) {
         return false;
      } else {
         si += 20;
         int ei = doc.indexOf("<", si);
         String sClosed = doc.substring(si, ei);
         this.kernel.setWebmoduleAccess(!Boolean.valueOf(sClosed));
         return true;
      }
   }

   private ByteArrayOutputStream doWork(ByteArrayOutputStream doc, String alias, String clientIP) {
      SXMLClient c = new SXMLClient();
      AppAliasData ad = this.kernel.getAppData();

      ByteArrayOutputStream var12;
      try {
         String sErr;
         ByteArrayOutputStream result;
         try {
            if (doc == null || doc.size() == 0) {
               sErr = "<engisoft_eit><proc_result type=\"error\"><ErrorText>Error en el servlet:\n";
               sErr = sErr + "Invalid document (null).";
               sErr = sErr + "</ErrorText></proc_result></engisoft_eit>";
               var12 = EStringUtils.stringToArray(sErr);
               return var12;
            }

            int st = ad.getAliasConfig(alias);
            if (st == 0) {
               st = c.login(ad.ip, ad.port, ad.appName, ad.fdsApp, clientIP, ad.loadLimitMode);
               if (st != 0) {
                  String sErrApp = "EITech Alias: " + alias + "\n";
                  sErrApp = sErrApp + "Server: " + ad.ip + "\n";
                  sErrApp = sErrApp + "Port: " + ad.port + "\n";
                  sErrApp = sErrApp + "FDSAPP: " + ad.fdsApp + "\n";
                  sErrApp = sErrApp + "Application: " + ad.appName + "\n";
                  sErrApp = sErrApp + "# Error: " + st;
                  sErr = "<engisoft_eit><proc_result type=\"error\" layer=\"fdserver\"><ErrorText Error=\"2\" app=\"" + sErrApp + "\">";
                  sErr = sErr + "The Servlet module at the HTTP server has not been able to connect with FDServer.\n" + sErrApp;
                  sErr = sErr + "\nError ID: " + Long.toString(ErrorContextBuilder.getCurrentErrorId());
                  sErr = sErr + "</ErrorText></proc_result></engisoft_eit>";
                  var12 = EStringUtils.stringToArray(sErr);
                  return var12;
               }

               result = c.execFunc(doc);
               if (result == null || result.size() == 0) {
                  sErr = "<engisoft_eit><proc_result type=\"error\"><ErrorText>Error en el servlet:\n";
                  sErr = sErr + "Retorno de la funcion incorrecto.";
                  sErr = sErr + "\nError ID: " + Long.toString(ErrorContextBuilder.getCurrentErrorId());
                  sErr = sErr + "</ErrorText></proc_result></engisoft_eit>";
                  result = EStringUtils.stringToArray(sErr);
               }

               return result;
            }

            sErr = "<engisoft_eit><proc_result type=\"error\" layer=\"eitconfig\"><ErrorText Error=\"" + String.valueOf(st);
            sErr = sErr + "\" file=\"Servlet\" app=\"" + alias + "\">";
            switch(st) {
            case 2:
               sErr = sErr + "The requested application " + alias + " is not found in the application configuration file Servlet on the HTTP server.";
            default:
               sErr = sErr + "</ErrorText></proc_result></engisoft_eit>";
               var12 = EStringUtils.stringToArray(sErr);
            }
         } catch (Throwable var23) {
            this.PrintExceptionHeader(var23);
            var23.printStackTrace();
            sErr = "<engisoft_eit><proc_result type=\"error\"><ErrorText>Error en el servlet:\n";
            sErr = sErr + "Excepcion lanzada.";
            sErr = sErr + "\nError ID: " + Long.toString(ErrorContextBuilder.getCurrentErrorId());
            sErr = sErr + "</ErrorText></proc_result></engisoft_eit>";
            result = EStringUtils.stringToArray(sErr);
            return result;
         }
      } finally {
         try {
            c.logout();
         } catch (IOException var22) {
         }

      }

      return var12;
   }

   private void PrintExceptionHeader(Throwable e) {
      Date d = new Date();
      System.out.println("----------------------------------------------------");
      System.out.println("Servlet nhentrystd. Time: " + d);
      System.out.println("Se ha producido una excepci�n atrapada.");
      System.out.println("Excepcion: " + e.getMessage());
      System.out.println("----------------------------------------------------");
   }

   public void setMaxcon(int maxcon) {
      this.maxCon = maxcon;
      this.lim.setMaxValue(this.maxCon);
   }

   public void setTimeout(int timeout) {
      this.limTimeout = timeout;
   }

   public void clearCounters() {
      this.lim.clearCounters();
   }

   public void clearHistory() {
      this.lim.clearHistory();
   }

   public String getHTMLStatus() {
      return GETStatusServlet.getHTML(this.lim, this.maxCon, this.limTimeout);
   }

   public void reloadApps() {
      this.kernel.loadAppData();
   }

   public void reloadConfig() {
      this.kernel.loadServletConfig();
   }
}