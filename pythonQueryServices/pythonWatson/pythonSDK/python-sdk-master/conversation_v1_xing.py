import json
from watson_developer_cloud import ConversationV1

#########################
# message
#########################

conversation = ConversationV1(
    username='1a469652-f283-485f-a384-3db3a9d977d2',
    password='eIpqVjMM3rpS',
    version='2017-04-21')

# replace with your own workspace_id
workspace_id = '0b247c34-a47a-4562-8a03-36e0c683eea8'

#response = conversation.message(workspace_id=workspace_id, message_input={
#    'text': 'What\'s the weather like?'})
response = conversation.message(workspace_id=workspace_id, message_input={
    'text': 'Olly I want to set an alarms wednesday at five pm?'})
# could you set the alarm for me for doctor's appointment at five pm night please?
print(json.dumps(response, indent=2))

# When you send multiple requests for the same conversation, include the
# context object from the previous response.
# response = conversation.message(workspace_id=workspace_id, message_input={
# 'text': 'turn the wipers on'},
#                                context=response['context'])
# print(json.dumps(response, indent=2))

#########################
# workspaces
#########################

