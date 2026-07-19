import re

with open('app/src/main/AndroidManifest.xml', 'r') as f:
    content = f.read()

content = content.replace(
    '</application>',
    '''        <service
            android:name=".FCMService"
            android:exported="true">
            <intent-filter>
                <action android:name="com.google.firebase.MESSAGING_EVENT" />
            </intent-filter>
        </service>
    </application>'''
)

with open('app/src/main/AndroidManifest.xml', 'w') as f:
    f.write(content)
