$word = New-Object -ComObject Word.Application
$word.Visible = $true
$doc = $word.Documents.Open("C:\Users\antonette\Downloads\DEFENSE_ALL_QUESTIONS_AND_ANSWERS.docx")
$word.Activate()
