#!/bin/bash

function kgeorgiy_clone(){
    if [[ ! -e java-advanced-2019 ]]; then
        echo "Are you sure it is your working directory?"
        pwd
        read are_you_sure
        if [[ ! ${are_you_sure} == [Yy]* ]]; then
            return 1
        fi
        git clone https://www.kgeorgiy.info/git/geo/java-advanced-2019.git
        STATUS0="${?}"
        if [[ "$STATUS0" != "0" ]]; then
            echo error with kgeorgiy repo
            return 1
        fi
    else
        cd java-advanced-2019/
        git pull
        STATUS0="${?}"
        if [[ "$STATUS0" != "0" ]]; then
            echo error with kgeorgiy repo
            return 1
        fi
        cd ..
    fi
    rm README.md 2>/dev/null
    cp java-advanced-2019/README.md .
    echo ""
    return 0
}

function kgeorgiy_compile(){
    echo "compiling ru/ifmo/rain/$1/$2/"
    echo
    javac ru/ifmo/rain/$1/$2/*.java $3 $4
    STATUS="${?}"
    if [[ "$STATUS" != "0" ]]; then
        echo compilation error
        return 1
    fi
    echo
    echo compiled
    return 0
}

function kgeorgiy(){

    IS_TEST="false"
    STUDENT="lundin"
    JAVADOC="false"
    CREATE_JAR="false"

    while [[ $# -gt 0 ]]
    do
        case "$1" in

      -jar)
        CREATE_JAR="true"
        shift
        break
        ;;
      -git)
		./clear 2>/dev/null
        git add .
        shift
        git commit -m \"$@\"
        git push
        return 0
        ;;
       --help)
        echo Usage:
        echo kgeorgiy "[launch options]"
        echo
        echo where options include:
        echo
        echo no options to test your task
        echo -jar to create jar
        echo -doc create javadoc for
        echo -test to test your hw 
        echo     your file should include "main()"
        echo -ou only update kgeorgiy repo
        echo -u update and run test
        echo -add add launch options 
        return 0
        ;;
            -test)
                IS_TEST="true"
                shift
                break
                ;;
            -doc)
                JAVADOC="true"
                shift
                break
                ;;
            -ou)
                kgeorgiy_clone
                return 0
                shift
                ;;
            -u)
                kgeorgiy_clone
                shift
                ;;
            -add)
                shift
                echo "add"
                if [[ "$#" -ne 4 ]]; then
                    echo "Illegal number of parameters"
                    return 1
                fi
                echo $1:$2:$3:$4 >> launch_options.data
                echo "done"
                return 0
                break;
                ;;
            *)
                break
        esac
    done

   if ! [[ -d "java-advanced-2019" ]]; then
        echo doesnt exist
        echo cloning repo
        kgeorgiy_clone
        STATUS_CLONE="${?}"
        if [[ "$STATUS_CLONE" != "0" ]]; then
            echo aborting
            return 1
        fi
    fi

    echo "Enter task number"
    read number
    while [[ ! ${number} =~ ^[0-9]+$ ]]
    do
        echo "error: Not a number"
        read number
    done

    jar_include=""
    jar_include+=" -cp "
    for jar in java-advanced-2019/lib/*.jar
        do
            jar_include+=":"
            jar_include+=${jar};
        done
    for jar in java-advanced-2019/artifacts/*.jar
        do
            jar_include+=":"
            jar_include+=${jar};
        done

    if [[ ${IS_TEST} != "true" ]] && [[ ${JAVADOC} != "true" ]]; then
        echo "easy/hard?"
        read is_hard
    fi
    number1=${number}
    number+="p"
    PACKAGE=$(sed ${number} -n launch_options.data | awk -F ":" '{print $1}')
    easy=$(sed ${number} -n launch_options.data | awk -F ":" '{print $2}')
    hard=$(sed ${number} -n launch_options.data | awk -F ":" '{print $3}')
    PROGNAME=$(sed ${number} -n launch_options.data | awk -F ":" '{print $4}')


    kgeorgiy_compile ${STUDENT} ${PACKAGE} ${jar_include}
    STATUS="${?}"

    if [[ "$STATUS" != "0" ]]; then
        return 1
    fi

    if [[ ${number1} == "8" ]]; then
        number2="7p"
        PACKAGE1=$(sed ${number2} -n launch_options.data | awk -F ":" '{print $1}')
        PROGNAME1=$(sed ${number2} -n launch_options.data | awk -F ":" '{print $4}')

        kgeorgiy_compile ${STUDENT} ${PACKAGE1} ${jar_include}
        STATUS="${?}"

        if [[ "$STATUS" != "0" ]]; then
            return 1
        fi

        if [[ ${JAVADOC} == "true" ]]; then
            folder_doc=${number1}
            folder_doc+="doc/"
            mkdir ${folder_doc}

            javadoc ${jar_include} -link https://docs.oracle.com/en/java/javase/11/docs/api/ ru/ifmo/rain/${STUDENT}/${PACKAGE}/${PROGNAME}.java -d ${folder_doc} $@
            return 0
        fi

        if [[ ${IS_TEST} == "true" ]]; then
            java ${jar_include} -p ".:java-advanced-2019/artifacts/:java-advanced-2019/lib/" ru.ifmo.rain.${STUDENT}.${PACKAGE}.${PROGNAME} $@
            return 0
        fi

        if [[ ${is_hard} == [Ee]* ]]; then
            is_hard=${easy}
        else
            is_hard=${hard}
        fi

        java ${jar_include} -p ".:java-advanced-2019/artifacts/:java-advanced-2019/lib/" -m info.kgeorgiy.java.advanced.${PACKAGE} ${is_hard} ru.ifmo.rain.${STUDENT}.${PACKAGE}.${PROGNAME}","ru.ifmo.rain.${STUDENT}.${PACKAGE1}.${PROGNAME1} $@
        return 0
    fi

    if [[ ${IS_TEST} != "true" ]] && [[ ${JAVADOC} != "true" ]]; then
        if [[ ${is_hard} == [Ee]* ]]; then
            is_hard=${easy}
        else
            is_hard=${hard}
        fi

        if [[ ${number1} == "5" ]] && [[ ${CREATE_JAR} == "true" ]]; then
            echo oh.. task with jar..
            jar xf java-advanced-2019/artifacts/info.kgeorgiy.java.advanced.implementor.jar info/kgeorgiy/java/advanced/implementor/Impler.class info/kgeorgiy/java/advanced/implementor/JarImpler.class info/kgeorgiy/java/advanced/implementor/ImplerException.class
            jar cfm implementor_runnable.jar ru/ifmo/rain/${STUDENT}/${PACKAGE}/MANIFEST.MF ru/ifmo/rain/${STUDENT}/implementor/Implementor.class info/kgeorgiy/java/advanced/implementor/*.class
            echo jar created
            rm -rf info
            java -jar implementor_runnable.jar -jar info.kgeorgiy.java.advanced.implementor.Impler out.jar
            return 0
        fi
    fi
    if [[ ${JAVADOC} == "true" ]]; then
        folder_doc=${number1}
        folder_doc+="doc/"
        mkdir ${folder_doc}

        javadoc ${jar_include} -link https://docs.oracle.com/en/java/javase/11/docs/api/ ru/ifmo/rain/${STUDENT}/${PACKAGE}/${PROGNAME}.java -d ${folder_doc} $@
        return 0
    fi

    if [[ ${IS_TEST} == "true" ]]; then
        java ${jar_include} -p ".:java-advanced-2019/artifacts/:java-advanced-2019/lib/" ru.ifmo.rain.${STUDENT}.${PACKAGE}.${PROGNAME} $@
        return 0
    fi

    java ${jar_include} -p ".:java-advanced-2019/artifacts/:java-advanced-2019/lib/" -m info.kgeorgiy.java.advanced.${PACKAGE} ${is_hard} ru.ifmo.rain.${STUDENT}.${PACKAGE}.${PROGNAME} $@
    return 0
}