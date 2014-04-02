"""
CPAchecker is a tool for configurable software verification.
This file is part of CPAchecker.

Copyright (C) 2007-2014  Dirk Beyer
All rights reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.


CPAchecker web page:
  http://cpachecker.sosy-lab.org
"""

# prepare for Python 3
from __future__ import absolute_import, print_function, unicode_literals

import glob
import logging
import os
import signal
import subprocess
import sys
import xml.etree.ElementTree as ET

"""
This module contains some useful functions for Strings, XML or Lists.
"""

def isWindows():
    return os.name == 'nt'

def forceLinuxPath(path):
    if isWindows():
        return path.replace('\\', '/')
    return path

def killProcess(pid, sig=signal.SIGTERM):
    '''
    This function kills the process and the children in its process group.
    '''
    try:
        os.killpg(pid, sig)
    except OSError: # process itself returned and exited before killing
        pass

def printOut(value, end='\n'):
    """
    This function prints the given String immediately and flushes the output.
    """
    sys.stdout.write(value)
    sys.stdout.write(end)
    sys.stdout.flush()

def isCode(filename):
    """
    This function returns True, if  a line of the file contains bracket '{'.
    """
    isCodeFile = False
    file = open(filename, "r")
    for line in file:
        # ignore comments and empty lines
        if not isComment(line) \
                and '{' in line: # <-- simple indicator for code
            if '${' not in line: # <-- ${abc} variable to substitute
                isCodeFile = True
    file.close()
    return isCodeFile


def isComment(line):
    return not line or line.startswith("#") or line.startswith("//")


def containsAny(text, list):
    '''
    This function returns True, iff any string in list is a substring of text.
    '''
    for elem in list:
        if elem in text:
            return True
    return False


def removeAll(list, elemToRemove):
    return [elem for elem in list if elem != elemToRemove]


def flatten(iterable, exclude=[]):
    return [value for sublist in iterable for value in sublist if not value in exclude]


def getListFromXML(elem, tag="option", attributes=["name"]):
    '''
    This function searches for all "option"-tags and returns a list with all attributes and texts.
    '''
    return flatten(([option.get(attr) for attr in attributes] + [option.text] for option in elem.findall(tag)), exclude=[None])


def getCopyOfXMLElem(elem):
    """
    This method returns a shallow copy of a XML-Element.
    This method is for compatibility with Python 2.6 or earlier..
    In Python 2.7 you can use  'copyElem = elem.copy()'  instead.
    """

    copyElem = ET.Element(elem.tag, elem.attrib)
    for child in elem:
        copyElem.append(child)
    return copyElem


def XMLtoString(elem):
    """
    Return a pretty-printed XML string for the Element.
    """
    from xml.dom import minidom
    rough_string = ET.tostring(elem, 'utf-8')
    reparsed = minidom.parseString(rough_string)
    return reparsed.toprettyxml(indent="  ")


def decodeToString(toDecode):
    """
    This function is needed for Python 3,
    because a subprocess can return bytes instead of a string.
    """
    try:
        return toDecode.decode('utf-8')
    except AttributeError: # bytesToDecode was of type string before
        return toDecode


def formatNumber(number, numberOfDigits):
    """
    The function formatNumber() return a string-representation of a number
    with a number of digits after the decimal separator.
    If the number has more digits, it is rounded.
    If the number has less digits, zeros are added.

    @param number: the number to format
    @param digits: the number of digits
    """
    if number is None:
        return ""
    return "%.{0}f".format(numberOfDigits) % number


def expandFileNamePattern(pattern, baseDir):
    """
    Expand a file name pattern containing wildcards, environment variables etc.

    @param pattern: The pattern string to expand.
    @param baseDir: The directory where relative paths are based on.
    @return: A list of file names (possibly empty).
    """
    # 'join' ignores baseDir, if expandedPattern is absolute.
    # 'normpath' replaces 'A/foo/../B' with 'A/B', for pretty printing only
    pattern = os.path.normpath(os.path.join(baseDir, pattern))

    # expand tilde and variables
    pattern = os.path.expandvars(os.path.expanduser(pattern))

    # expand wildcards
    fileList = glob.glob(pattern)

    return fileList


def appendFileToFile(sourcename, targetname):
    source = open(sourcename, 'r')
    try:
        target = open(targetname, 'a')
        try:
            target.writelines(source.readlines())
        finally:
            target.close()
    finally:
        source.close()


def findExecutable(program, fallback=None, exitOnError=True):
    def isExecutable(programPath):
        return os.path.isfile(programPath) and os.access(programPath, os.X_OK)

    dirs = os.environ['PATH'].split(os.path.pathsep)
    dirs.append(os.path.curdir)

    for dir in dirs:
        name = os.path.join(dir, program)
        if isExecutable(name):
            return name

    if fallback is not None and isExecutable(fallback):
        return fallback

    if exitOnError:
        sys.exit("ERROR: Could not find '{0}' executable".format(program))
    else:
        return fallback


def commonBaseDir(l):
    # os.path.commonprefix returns the common prefix, not the common directory
    return os.path.dirname(os.path.commonprefix(l))


def addFilesToGitRepository(baseDir, files, description):
    """
    Add and commit all files given in a list into a git repository in the
    baseDir directory. Nothing is done if the git repository has
    local changes.

    @param files: the files to commit
    @param description: the commit message
    """
    if not os.path.isdir(baseDir):
        printOut('Output path is not a directory, cannot add files to git repository.')
        return

    # find out root directory of repository
    gitRoot = subprocess.Popen(['git', 'rev-parse', '--show-toplevel'],
                               cwd=baseDir,
                               stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    stdout = gitRoot.communicate()[0]
    if gitRoot.returncode != 0:
        printOut('Cannot commit results to repository: git rev-parse failed, perhaps output path is not a git directory?')
        return
    gitRootDir = decodeToString(stdout).splitlines()[0]

    # check whether repository is clean
    gitStatus = subprocess.Popen(['git','status','--porcelain', '--untracked-files=no'],
                                 cwd=gitRootDir,
                                 stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    (stdout, stderr) = gitStatus.communicate()
    if gitStatus.returncode != 0:
        printOut('Git status failed! Output was:\n' + decodeToString(stderr))
        return

    if stdout:
        printOut('Git repository has local changes, not commiting results.')
        return

    # add files to staging area
    files = [os.path.realpath(file) for file in files]
    gitAdd = subprocess.Popen(['git', 'add', '--'] + files,
                               cwd=gitRootDir)
    if gitAdd.wait() != 0:
        printOut('Git add failed, will not commit results!')
        return

    # commit files
    printOut('Committing results files to git repository in ' + gitRootDir)
    gitCommit = subprocess.Popen(['git', 'commit', '--file=-', '--quiet'],
                                 cwd=gitRootDir,
                                 stdin=subprocess.PIPE)
    gitCommit.communicate(description.encode('UTF-8'))
    if gitCommit.returncode != 0:
        printOut('Git commit failed!')
        return



def getEnergy():
    '''
    returns a current value of energy consumption (like a time-stamp)
    or None, if measurement is not available.
    '''
    executable = findExecutable('read-energy.sh', exitOnError=False)
    if executable is None: # not availableon current system
        return None

    # TODO support more ids like CPU, CORE, UNCORE, EXTERNAL, ...
    energysh = subprocess.Popen([executable, 'cpu'], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    (stdout, stderr) = energysh.communicate()
    if energysh.returncode:
        logging.warning('error while reading energy: out={0}, err={1}, retval={2}'.format(stdout, stderr, energysh.returncode))
    try:
        energyVal = int(stdout)
    except ValueError:
        return None

    return energyVal
