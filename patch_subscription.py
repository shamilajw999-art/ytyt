import re

with open('app/src/main/java/com/example/ui/screens/SubscriptionScreen.kt', 'r') as f:
    content = f.read()

pattern = re.compile(r"    val isLoading by subscriptionViewModel\.isLoading\.collectAsState\(\)")
new_code = """    val isLoading by subscriptionViewModel.isLoading.collectAsState()
    val entitlements by viewModel.entitlements.collectAsState()
    val activePlan = entitlements?.plan?.uppercase() ?: "FREE\""""

content = pattern.sub(new_code, content)

pattern2 = re.compile(r"            Text\(\n                text = \"Choose Your Mythic\",\n                color = Color\.White,\n                fontSize = 28\.sp,\n                fontWeight = FontWeight\.Bold\n            \)")
new_code2 = """            Text(
                text = "Choose Your Mythic",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF86FC5C).copy(alpha = 0.2f))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CURRENT PLAN: $activePlan",
                    color = Color(0xFF86FC5C),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }"""

content = pattern2.sub(new_code2, content)

with open('app/src/main/java/com/example/ui/screens/SubscriptionScreen.kt', 'w') as f:
    f.write(content)
