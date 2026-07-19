import re

with open('app/src/main/java/com/aistudio/micrhema/Screens.kt', 'r') as f:
    content = f.read()

target = """fun EditMembersSection() {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Aprovações e Níveis de Membros",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Gerencie quem tem acesso às abas VIP e IBR",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (memberRequestsState.isEmpty()) {"""

replacement = """@OptIn(ExperimentalMaterial3Api::class)
fun EditMembersSection() {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Todos") }
    
    val filteredMembers = remember(memberRequestsState.toList(), searchQuery, selectedFilter) {
        memberRequestsState.filter { member ->
            val matchesSearch = member.name.contains(searchQuery, ignoreCase = true) || member.email.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                "Aprovados" -> member.isApproved
                "Pendentes" -> !member.isApproved
                "VIP" -> member.isVip
                "IBR" -> member.isIbr
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Aprovações e Níveis de Membros",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Gerencie quem tem acesso às abas VIP e IBR",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        GlassTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Buscar membro por nome ou email") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filters = listOf("Todos", "Aprovados", "Pendentes", "VIP", "IBR")
            filters.forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (filteredMembers.isEmpty()) {"""

content = content.replace(target, replacement)

target2 = """            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(memberRequestsState) { req ->"""

replacement2 = """            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredMembers) { req ->"""
                
content = content.replace(target2, replacement2)

with open('app/src/main/java/com/aistudio/micrhema/Screens.kt', 'w') as f:
    f.write(content)
