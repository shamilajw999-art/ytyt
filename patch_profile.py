import re

with open('app/src/main/java/com/example/ui/screens/ProfileScreen.kt', 'r') as f:
    content = f.read()

pattern = re.compile(r"    val activeSubscription by subscriptionViewModel\.activeSubscription\.collectAsState\(\)")
new_code = """    val entitlements by viewModel.entitlements.collectAsState()"""
content = pattern.sub(new_code, content)

content = content.replace("activeSubscription != null", "(entitlements?.plan == \"pro\" || entitlements?.plan == \"max\")")
content = content.replace("activeSubscription == null", "(entitlements?.plan == \"free\" || entitlements == null)")
content = content.replace("activeSubscription?.plan", "entitlements?.plan")

with open('app/src/main/java/com/example/ui/screens/ProfileScreen.kt', 'w') as f:
    f.write(content)
