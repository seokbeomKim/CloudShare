# -*- coding: utf-8 -*-
"""
Ndrive File Downloader

@author: Sukbeom Kim


"""

import os, sys
from os.path import expanduser
import urllib, urllib2
import requests
import simplejson as json
import magic
import datetime
import re
import Cookie 
import cookielib 
import pdb
import time
from ghost import Ghost

def main(page_url):
    if page_url is None:
        return
    # Ghost 초기화
    ghost = Ghost()
    session = requests.session()
    
    r = session.get(page_url)
    r.encoding = 'utf-8'

    myvar = requests.args.get("gsSaveFileLink")
    print myvar
    sys.exit(0)
    # 초기화 후 ghost session 시작
    with ghost.start() as g_session:
        g_session.open(page_url)
        g_session.wait_for_page_loaded()
        g_session.evaluate("""
        (function() {
        document.getElementsByClassName('sh_fil_group2')[1].click();
        })();
        """)
        g_session.wait_for_selector('#query')
        request = g_session.get(url, params = data, stream=True)
        print request
    sys.exit(0)

if __name__ == '__main__':
    if len(sys.argv) != 2:
        sys.exit(1)
    main(sys.argv[1])
