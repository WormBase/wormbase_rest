#!/usr/bin/env python
from bulbs.model import Node, Relationship
from bulbs.property import String, Integer, DateTime
from bulbs.utils import current_datetime


####################################
# Variant B with flattened Evidence

#################################
# Gene class and related stuff
class Gene(Node):
	element_type = "Gene"
	name = String(nullable=False)
	created = DateTime(default=current_datetime, nullable=False)
	CGC_name = String()
	Sequence_name = String()
	Public_name = String()
	Concise_description = String()
	Concise_descriptionEvidence = String() # nonUnique

# Gene -> RNAi
class GeneRNAi_result(Relationship):
	created = DateTime(default=current_datetime, nullable=False)
	label   = "GeneRNAi_resultlt"
	Evidence = String()

# Gene -> Paper
class GeneReference(Relationship):
	created = DateTime(default=current_datetime, nullable=False)
	label   = "GeneReference"

###############################
# RNAi class
class RNAi(Node):
	element_type = "RNAi"
	name = String(nullable=False)
	created = DateTime(default=current_datetime, nullable=False)
	Strain = String()
	Delivered_by = String()
	Evidence = String()

# RNAi -> Gene
class RNAiGene(Relationship):
	label = "RNAiGene"
	created = DateTime(default=current_datetime, nullable=False)
	Evidence = String()

# RNAi -> Phenotype
class RNAiPhenotype(Relationship):
	label = "RNAiPhenotype"
	created = DateTime(default=current_datetime, nullable=False)
	Evidence = String()

# RNAi -> Phenotype_not_observed
class RNAiPhenotype_not_observed(Relationship):
	label = "RNAiPhenotype_not_observed"
	created = DateTime(default=current_datetime, nullable=False)
	Evidence = String()

# RNAi -> Reference
class RNAiReference(Relationship):
	label = "RNAiReference"
	created = DateTime(default=current_datetime, nullable=False)

###############################
# Phenotype class
class Phenotype(Node):
	Date_last_updated = DateTime(default=current_datetime, nullable=False)
	element_type = "Phenotype"
	name = String(nullable=False)
	Primary_name = String()
	Description = String()

# Phenotype -> RNAi
class PhenotypeRNAi(Relationship):
	label = "PhenotypeRNAi"
	created = DateTime(default=current_datetime, nullable=False)

# Phenotype -> Not_in_RNAi
class PhenotypeNot_in_RNAi(Relationship):
	label = "PhenotypeNot_in_RNAi"
	created = DateTime(default=current_datetime, nullable=False)

###############################
# Paper class
class Paper(Node):
	Date_last_updated = DateTime(default=current_datetime, nullable=False)
	element_type = "Paper"
	name = String(nullable=False)
	Author = String()
	Title = String()
	Journal = String()
	Volume = String()
	Page = String()
	Brief_citation = String()
	Abstract = String()

# Paper -> Gene
class PaperGene(Relationship):
	label = "PaperGene"
	created = DateTime(default=current_datetime, nullable=False)

# Paper -> RNAi
class PaperRNAi(Relationship):
	label = "PaperRNAi"
	created = DateTime(default=current_datetime, nullable=False)
