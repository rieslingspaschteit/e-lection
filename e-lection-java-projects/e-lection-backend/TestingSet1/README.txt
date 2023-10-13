In diesem Ordner sind Test-JSONs, mit denen das Backend über Postman getestet wurden. 
Dabei handelt es sich um eine Wahl mit zwei Trustees, bei der das erste Ballot submitted und das zweite gespoiled gespoiled (hochgeladen aber nicht submitted)wurde.

Anmerkung: 
1.) Damit das Testen deterministischer wurde, muss das Startdatum der Wahl in der Datenbank auf das Startdatum aus der CreationRequest gesetzt werden.
2.) Email-Adressen müssen ebenfalls angepasst werden.
3.) Es müssen beide Partial Decryptions abgegeben werden
4.) Da leider der Startzeitpunkt fix ist, müssen alle Methoden die einen Switch von Open in Dec_Phase_One ausüber auskommentiert werden, da sonst Fehlermeldungen geschickt werden
