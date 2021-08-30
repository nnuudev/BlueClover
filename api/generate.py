#!/usr/bin/env python
# -*- coding: utf-8 -*-

import json
import urllib.request
import xml.etree.ElementTree as ET

result = {
    'api_version': 1,
    'check_interval': 432000000,
    'messages': [ {
        'code': 0,
        'type': 'update'
        } ]
    }
message = result['messages'][0]

url = 'https://github.com/nnuudev/BlueClover/releases.atom'
atom = urllib.request.urlopen(url).read().decode('utf-8')

latest = ET.fromstring(atom).find('.//{http://www.w3.org/2005/Atom}entry')
message['date'] = latest.find('{http://www.w3.org/2005/Atom}updated').text[:-1]
title = latest.find('{http://www.w3.org/2005/Atom}title').text
message['message_html'] = '<h2>Blue Clover update ready</h2><b>' + title + '</b><br>' + latest.find('{http://www.w3.org/2005/Atom}content').text
id = latest.find('{http://www.w3.org/2005/Atom}id').text.split('/')[-1]
build = id.split('-')[-1]
message['hash'] = build
message['apk'] = {
    'default': {
        'url': 'https://github.com/nnuudev/BlueClover/releases/download/' + id + '/BlueClover-' + build + '.apk'
        }
    }

with open('output.tmp', 'wb') as out:
    out.write(json.dumps(result, ensure_ascii=False).encode('utf8'))