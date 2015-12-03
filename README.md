---

![TU Dortmund Logo](http://www.ub.tu-dortmund.de/images/tu-logo.png)

![UB Dortmund Logo](http://www.ub.tu-dortmund.de/images/ub-schriftzug.jpg)

---

# Implementierung der *Linked Data Platform* für die UA-Ruhr-Bibliographie

Hierbei handelt es sich um eine Implementierung der [Linked Data Platform](https://github.com/hagbeck/LinkedDataPlatform) auf Basis von [de.tu_dortmund.ub.hb_ng.data.SolRDF](https://github.com/agazzarini/de.tu_dortmund.ub.hb_ng.data.SolRDF) von Andrea Gazzarini.
Die Implementierung wird für das Backend des Projekts [hb_ng](https://github.com/ubbochum/hb_ng) verwendet.

## Verwendung der Endpoints:

Hole Ressource mit URI:

	GET /resource/...
	Accept: application/rdf+xml, application/ld+json, text/turtle, application/n-quads, application/rdf+n3, text/html
	[Authorization: ...]

	Hole eine Resource, die mit der Base-URL des Service bezeichnet ist, z.B. http://data.ub.tu-dortmund.de/resource/work/1234 für die Ressource <http://data.ub.tu-dortmund.de/resource/work/1234> (hier ist http://data.ub.tu-dortmund.de die URL der Plattform).
	
	GET /service/resource?uri=...
	Accept: application/rdf+xml, application/ld+json, text/turtle, application/n-quads, application/rdf+n3, text/html
	[Authorization: ...]
	
	Hole eine Ressource zu einem beliebigen URI

SPARQL-Query:

	GET /service/sparql?q={URLencoded SPARQL query}
	Accept: application/json, application/xml, application/rdf+xml, application/ld+json, text/turtle, application/n-quads, application/rdf+n3, text/html
	[Authorization: ...]

	POST /service/sparql
	Content-type: application/sparql-query
	Accept: application/json, application/xml, application/rdf+xml, application/ld+json, text/turtle, application/n-quads, application/rdf+n3, text/html
	[Authorization: ...]

SPARQL-Update:

	POST /service/sparql
	Content-type: application/sparql-update
	Authorization: ...
	
Beispieldaten für SPARQL-Update: [Test file for SPARQL-Update for hb_ng (GitHubGist)](https://gist.github.com/hagbeck/ca978632075b85e34adc)

Suche - **noch nicht implementiert**:

	GET /service/search?q=...[&fq=...][&start=...][&rows=...][&sort=...]
	Accept: application/xml, application/json, text/html


# Kontakt

**data@ubdo - Datenplattform der Universitätsbibliothek Dortmund**

Technische Universität Dortmund // Universitätsbibliothek // Bibliotheks-IT // Vogelpothsweg 76 // 44227 Dortmund

[Webseite](https://data.ub.tu-dortmund.de) // [E-Mail](mailto:opendata@ub.tu-dortmund.de)

---

![Creative Commons License](http://i.creativecommons.org/l/by/4.0/88x31.png)

This work is licensed under a [Creative Commons Attribution 4.0 International License (CC BY 4.0)](http://creativecommons.org/licenses/by/4.0/)
