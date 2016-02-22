#!/bin/sh
#------- ABSCHNITT : Modul Identifikation -------------------------------------
#
# Modul Name       : Waagenwatchservice.sh
# .     Verwendung : Waagen Schnittstelle
# Autor            : TK
# Verantwortlich   : TK
# Beratungspflicht : nein
# Copyright        : abas GmbH
#
#------- ABSCHNITT : Modul Beschreibung ---------------------------------------
#
#------------------------------------------------------------------------------

#------- ABSCHNITT : Defines, Prozeduren, Typen und Daten ---------------------
DEVNULL=${DEVNULL:-/dev/null}; export DEVNULL
name=`basename $0`
whoami=`id -un | sed 's/^.*\\\\//'`  # wg. Windows: DOMAIN\user -> user
pwd=`pwd`

myexit()
{
  exitcode=$1
  errmsg="$2"
  if [ -n "$errmsg" ];then
      echo $name: "$errmsg" >&2
  fi
  exit $1
}

#------------------------------------------------------------------------------
# zeigt die Usage an
#------------------------------------------------------------------------------
usage() {
  ech  "" >&2
  echo "usage: $name -cfg configurationfile [-stop]" >&2
  echo "   * Option -cfg:  configuration file" >&2
  echo "   * Option -stop: stop process to given configurationfile" >&2
  echo "" >&2
  exit 1
}

#------------------------------------------------------------------------------
# Pruefung auf Mandantenverzeichnis
#------------------------------------------------------------------------------
mandtest.sh -f || exit 1
waage_mandantdir=`pwd`

#------- ABSCHNITT : Optionen -------------------------------------------------
# Options- und Nichtoptionsargumente koennen Blanks enthalten.

configfile=
stopping=false

waage_logging_props=${waage_mandantdir}/java/log/config/logging.properties

while [ $# -gt 0 ]
do
  case "$1" in
   -cfg)  configfile="$2"
          shift
          ;;
   -stop) stopping=true
          ;;
    -*) usage
        ;;
     *) break
        ;;
  esac
  shift
done

[ -z "$configfile" ] && usage
[ -f $configfile ] || myexit 1 "configuration file not found: $configfile"
[ -f $waage_logging_props ] || myexit "logging.properties not found: $pdm_logging_props"

#------- ABSCHNITT : Hauptprogramm --------------------------------------------
# main

if [ $stopping == true ]; then
  $JAVAPATH/bin/java -Dlog4j.configuration=file:${pdm_logging_props} -jar ${HOMEDIR}/java/lib/waage-watchservice.jar -cfg $configfile -stop &
else
  $JAVAPATH/bin/java -Dlog4j.configuration=file:${pdm_logging_props} -jar ${HOMEDIR}/java/lib/waage-watchservice.jar -cfg $configfile &
fi

exit 0
