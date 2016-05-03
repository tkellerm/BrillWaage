/*-----------------------------------------------------------------------------
 * Modul Name       : PdmWatchService.java
 * Verwendung       : Verzeichnisueberwachung fuer PDM-Schnittstelle
 * Autor            : uko
 * Verantwortlich   : uko
 * Kontrolle        : dsch
 * Beratungspflicht : nein
 *---------------------------------------------------------------------------*/

package de.abasgmbh.brill.waage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.net.ConnectException;
import java.net.UnknownHostException;

import org.apache.log4j.Logger;

import de.abasgmbh.brill.waage.config.WaageConfiguration;
import de.abasgmbh.brill.waage.config.WaageConfigurationReader;



public class WaageWatchService {

    private final static String myPID = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];

    private final static Logger log = Logger.getLogger(WaageWatchService.class);

    private static final String ARGUMENT_CFGFILE = "-cfg";
    private static String configfile = null;
    private static File pidfile = null;
    private static final String ARGUMENT_STOP = "-stop";
    private static boolean stopService = false;
    private static final String ARGUMENT_TEST = "-test";
    private static boolean testService = false;

    private static WaageConfigurationReader waageConfigurationReader = WaageConfigurationReader.getInstance();
    private static WaageConfiguration configuration = WaageConfiguration.getInstance();
    

    private static void exit(int exitCode, final String exitMessage) {
        if (exitCode == 0) {
            log.info(exitMessage);
            System.out.println("[INFO] " + exitMessage);
        } else {
            log.error(exitMessage);
            System.err.println("[ERROR] " + exitMessage);
            System.out.println("[ERROR] " + exitMessage);
        }
        if (pidfile != null) {
        	pidfile.delete();	
		}
        
        Runtime.getRuntime().exit(exitCode);
    }

    private static void exit(int exitCode, final Throwable e) {
        log.error(e.getMessage(), e);
        System.err.println("[ERROR] " + e.getMessage());
        if (pidfile != null) {
        	pidfile.delete();	
		}
        try {
			Thread.sleep(10000);
		} catch (InterruptedException e1) {
			log.error("Beim Beenden konnte der Thread nicht schlafen gelegt werden!" , e1);
		}
        Runtime.getRuntime().exit(exitCode);
    }

    private static void exit(int exitCode, String exitMessage, Throwable e) {
        log.error(exitMessage + " # " + e.getMessage(), e);
        System.err.println("[ERROR] " + exitMessage + " # " + e.getMessage());
        if (pidfile != null) {
        	pidfile.delete();	
		}
        try {
			Thread.sleep(10000);
		} catch (InterruptedException e1) {
			log.error("Beim Beenden konnte der Thread nicht schlafen gelegt werden!" , e1);
		}
        Runtime.getRuntime().exit(exitCode);
    }

    /**
     *
     * @param args
     *            args[0] : configuration file
     * @throws ConnectException 
     */
    public static void main(String[] args) {
        /*
         * Arguments
         */
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals(ARGUMENT_CFGFILE)) {
                configfile = args[++i];
            } else if (args[i].equals(ARGUMENT_STOP)) {
                stopService = true;
            } else if (args[i].equals(ARGUMENT_TEST)) {
                testService= true;
            } else {
                System.out.println("Unknown parameter '" + args[i] + "' will be ignored.");
            }
        }
        if (!stopService) {
            log.info("[" + myPID + "] ####### NEW WATCHSERVICE #######");
        }

        // configuration file
        if (null == configfile) {
            exit(1, "No configuration file given.");
        }
        
        File cfgFile = new File(configfile);
        if (!cfgFile.exists()) {
            exit(1, "Configuration file not found: " + cfgFile.getAbsolutePath());
        }
        if (!cfgFile.isFile()) {
            exit(1, "Given file is no file: " + cfgFile.getAbsolutePath());
        }

        // PID file
        setPidfile(cfgFile);
        if (pidfile.exists()) {
            int runningPID = getPidFromFile(pidfile);
            if (processIsAlreadyRunning(runningPID, cfgFile.getName())) {
                if (stopService) {
                    log.info("stopping process...");
                    try {
                        Runtime.getRuntime().exec("kill -9 " + runningPID);
                        pidfile.delete();
                    } catch (IOException e1) {
                        exit(1, "[" + runningPID + "] Could not stop service.", e1);
                    }
                    exit(0, "[" + runningPID + "] WaagenWatchService stopped.");
                }
                exit(1, "[error] Process already running (pid " + runningPID + ") ...");
            } else {
                if (stopService) {
                	pidfile.delete();
                    exit(0, "Specified WaagenWatchService is not running.");
                }
                // else: Write PID to PID file
            }
        }
        // write PID to PID file
        FileWriter w = null;
        try {
            w = new FileWriter(pidfile);
            w.write(myPID);
        } catch (IOException e) {
            exit(1, "IOException in pid file.", e);
        } finally {
            try {
                w.close();
            } catch (IOException e) {
            }
        }
        
//      AbasRueckmeldung erzeugen
        
        if (!stopService) {
			/*
			 * Configuration
			 */
			try {

				waageConfigurationReader.read(cfgFile);
				configuration = waageConfigurationReader.getConfiguration();
				configuration.setPIDFile(pidfile);

			} catch (IOException e) {
				exit(1, e);
			}
			/*
			 * Existing files
			 */
			log.info("Handle existing files...");
			log.info("Starting WaagenWatchservice...");
			AbasRueckmeldung abasrueck;
			Thread threadAbas = new Thread(abasrueck = new AbasRueckmeldung(
					waageConfigurationReader.getConfiguration()));
			threadAbas.start();
			Long wartezaehler = new Long(0);
			if (abasrueck == null) {
				throw new NullPointerException(
						"Es wurde kein Object abasRückmeldung angelegt!");
			}
			while (!abasrueck.isConnected()) {
				wartezaehler = wartezaehler + 1;
				if (wartezaehler % 100 == 0) {
					log.info(wartezaehler.toString()
							+ "   Warten bis EDP-Verbindung steht");
				}
				if (wartezaehler > 10000000) {
					exit(1,
							"Es konnte keine EDP-Verbindung hergestellt werden! Der Waagen service hat sich beendet!");
				}
			}
			if (threadAbas.isAlive()) {
				log.info("abasrueckmeldung Thread wurde erzeugt");
				//			Die einzelnen Waagenüberwachungen starten
				boolean alive = threadAbas.isAlive();
				try {

					for (int i = 0; i < waageConfigurationReader
							.getConfiguration().getAnzahlWaagen(); i++) {
						log.trace("WaageThread " + i + " wird gestartet");
						SocketClient socket = new SocketClient(
								waageConfigurationReader.getConfiguration(), i,
								abasrueck);
					}
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					log.error(e);
				}
			} else {
				System.err
						.println("abasrueckmeldung Thread konnte nicht erzeugt werden");
				log.error("abasrueckmeldung Thread konnte nicht erzeugt werden");
			}
		}
		
//      

        

    }

    private static void setPidfile(File cfgFile) {
        String[] cF = configfile.split("/");
        String cFName = cF[cF.length - 1];
        cFName = cFName.substring(0, cFName.lastIndexOf("."));
        if (null == cfgFile.getParent()) {
            pidfile = new File(cFName + ".pid");
        } else {
            pidfile = new File(cfgFile.getParent().toString() + '/' + cFName + ".pid");
        }
        cFName = null;
        cF = null;
    }

    private static boolean processIsAlreadyRunning(int pid, String cfgFile) {
        log.debug("processIsAlreadyRunning(" + pid + ", " + cfgFile + ") ...");
        if (pid == 0) {
            exit(1, "PID file corrupted. Please check WatchService manually.");
        }

        String jarName = System.getProperty("java.class.path");
        jarName = jarName.split("/")[jarName.split("/").length - 1];
        String[] cmd = { "/bin/sh", "-c",
                "ps -o \"pid user command\" -p " + pid + " | sed 1d | grep -E '.*" + jarName + ".* -cfg .*" + cfgFile + ".*' | wc -l" };
        if (cmd.length >= 3) {
            log.debug(cmd[2]);
        }
        Process p = null;
        try {
            p = Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
            exit(1, "IOException in ps command.", e);
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
        int numOfLines = 0;
        try {
            numOfLines = Integer.valueOf(br.readLine());
            if (numOfLines >= 1) {
                return true;
            }
            return false;
        } catch (NumberFormatException e) {
            log.error("NumberFormatException in numOfLines of BufferedReader.", e);
        } catch (IOException e) {
            log.error("IOException in BufferedReader", e);
        } finally {
            try {
                br.close();
            } catch (IOException e) {
            }
        }

        return false;
    }

    private static int getPidFromFile(File pidfile) {
        int runningPID = 0;
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(pidfile));
            runningPID = Integer.valueOf(br.readLine());
        } catch (IOException e) {
            exit(1, e);
        } finally {
            try {
                br.close();
            } catch (IOException e) {
            }
        }
        return runningPID;
    }

    
    

}
