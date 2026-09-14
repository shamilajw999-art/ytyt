import re

with open('app/src/main/java/com/example/viewmodel/SubscriptionViewModel.kt', 'r') as f:
    content = f.read()

old_code = """                repository.createPaymentOrder(paymentOrder)
                createdOrder = paymentOrder"""

new_code = """                createdOrder = repository.createPaymentOrder(paymentOrder) ?: paymentOrder"""

content = content.replace(old_code, new_code)

with open('app/src/main/java/com/example/viewmodel/SubscriptionViewModel.kt', 'w') as f:
    f.write(content)
