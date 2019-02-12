import json
from watson_developer_cloud import NaturalLanguageUnderstandingV1
import watson_developer_cloud.natural_language_understanding.features.v1 as \
    Features


natural_language_understanding = NaturalLanguageUnderstandingV1(
    version='2017-02-27',
    username='1a469652-f283-485f-a384-3db3a9d977d2',
    password='eIpqVjMM3rpS')

response = natural_language_understanding.analyze(
    text='Olly I want to set an alarms wednesday at five pm',
    features=[Features.Entities(), Features.Keywords()])

print(json.dumps(response, indent=2))
