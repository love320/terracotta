<DOCFLEX_TEMPLATE VER='1.7'>
CREATED='2005-04-26 03:31:00'
LAST_UPDATE='2006-10-09 06:34:49'
DESIGNER_TOOL='DocFlex SDK 1.0'
TEMPLATE_TYPE='DocumentTemplate'
DSM_TYPE_ID='xsddoc'
ROOT_ET='xs:attribute'
<TEMPLATE_PARAMS>
	PARAM={
		param.name='nsURI';
		param.displayName='Target Namespace URI';
		param.type='string';
		param.defaultExpr='schema = getXMLDocument().findChild ("xs:schema");\nschema.getAttrStringValue("targetNamespace")';
		param.hidden='true';
	}
	PARAM={
		param.name='qName';
		param.description='The <code>QName</code> object representing the attribute qualified name. <i>(Since this template is supposed to document only global attributes, their names are always qualified).</i>\n<p>\nSee Expr. Assistant | XML Functions | <code>QName()</code> function.';
		param.type='object';
		param.defaultExpr='QName (getStringParam("nsURI"), getAttrStringValue("name"))';
	}
	PARAM={
		param.name='usageCount';
		param.description='number of locations where this global attribute is used';
		param.type='int';
		param.defaultExpr='countElementsByKey (\n  "attribute-usage",\n  getParam("qName")\n)';
		param.hidden='true';
	}
	PARAM={
		param.name='page.refs';
		param.displayName='Generate page references';
		param.type='boolean';
		param.boolean.default='true';
	}
	PARAM={
		param.name='doc.attribute.profile';
		param.displayName='Attribute Profile';
		param.type='boolean';
		param.boolean.default='true';
	}
	PARAM={
		param.name='doc.attribute.xmlRep';
		param.displayName='XML Representation Summary';
		param.type='boolean';
		param.boolean.default='true';
	}
	PARAM={
		param.name='doc.attribute.list.usage';
		param.displayName='Usage Locations';
		param.type='boolean';
		param.boolean.default='true';
	}
	PARAM={
		param.name='doc.attribute.annotation';
		param.displayName='Annotation';
		param.type='boolean';
		param.boolean.default='true';
	}
	PARAM={
		param.name='doc.attribute.embeddedType';
		param.displayName='Embedded Type Detail';
		param.type='boolean';
		param.boolean.default='true';
	}
	PARAM={
		param.name='doc.attribute.xmlSource';
		param.displayName='XML Source';
		param.type='boolean';
		param.boolean.default='true';
	}
	PARAM={
		param.name='sec.xmlSource.box.component';
		param.displayName='Components';
		param.type='boolean';
		param.boolean.default='true';
	}
</TEMPLATE_PARAMS>
<HTARGET>
	TARGET_KEYS={
		'contextElement.id';
		'"detail"';
		'"def"';
	}
</HTARGET>
<HTARGET>
	TARGET_KEYS={
		'getParam("qName")';
		'"attribute"';
	}
</HTARGET>
FMT={
	doc.lengthUnits='pt';
	doc.hlink.style.link='cs4';
}
<STYLES>
	CHAR_STYLE={
		style.name='Code';
		style.id='cs1';
		text.font.name='Courier New';
		text.font.size='9';
	}
	CHAR_STYLE={
		style.name='Code Smaller';
		style.id='cs2';
		text.font.name='Courier New';
		text.font.size='8';
	}
	CHAR_STYLE={
		style.name='Default Paragraph Font';
		style.id='cs3';
		style.default='true';
	}
	PAR_STYLE={
		style.name='Detail Heading 1';
		style.id='s1';
		text.font.size='12';
		text.font.style.bold='true';
		par.bkgr.opaque='true';
		par.bkgr.color='#CCCCFF';
		par.border.style='solid';
		par.border.color='#666666';
		par.margin.top='12';
		par.margin.bottom='10';
		par.padding.left='2.5';
		par.padding.right='2.5';
		par.padding.top='2';
		par.padding.bottom='2';
		par.page.keepWithNext='true';
	}
	PAR_STYLE={
		style.name='Detail Heading 2';
		style.id='s2';
		text.font.size='10';
		text.font.style.bold='true';
		par.bkgr.opaque='true';
		par.bkgr.color='#EEEEFF';
		par.border.style='solid';
		par.border.color='#666666';
		par.margin.top='12';
		par.margin.bottom='8';
		par.padding.left='2';
		par.padding.right='2';
		par.padding.top='2';
		par.padding.bottom='2';
		par.page.keepWithNext='true';
	}
	PAR_STYLE={
		style.name='Detail Heading 3';
		style.id='s3';
		text.font.size='9';
		text.font.style.bold='true';
		text.font.style.italic='true';
		text.color.background='#CCCCFF';
		text.color.opaque='true';
		par.margin.top='10';
		par.margin.bottom='8';
	}
	CHAR_STYLE={
		style.name='Hyperlink';
		style.id='cs4';
		text.decor.underline='true';
		text.color.foreground='#0000FF';
	}
	PAR_STYLE={
		style.name='Inset Heading 2';
		style.id='s4';
		text.font.size='9';
		text.font.style.bold='true';
		text.color.foreground='#990000';
		par.margin.bottom='6.8';
	}
	PAR_STYLE={
		style.name='List Heading 2';
		style.id='s5';
		text.font.name='Arial';
		text.font.size='9';
		text.font.style.bold='true';
		par.margin.bottom='8';
	}
	PAR_STYLE={
		style.name='Main Heading';
		style.id='s6';
		text.font.name='Verdana';
		text.font.size='13';
		text.font.style.bold='true';
		text.color.foreground='#4477AA';
		par.bkgr.opaque='true';
		par.bkgr.color='#EEEEEE';
		par.border.style='solid';
		par.border.color='#4477AA';
		par.margin.top='0';
		par.margin.bottom='9';
		par.padding.left='5';
		par.padding.right='5';
		par.padding.top='3';
		par.padding.bottom='3';
		par.page.keepWithNext='true';
	}
	PAR_STYLE={
		style.name='Normal';
		style.id='s7';
		style.default='true';
	}
	CHAR_STYLE={
		style.name='Normal Smaller';
		style.id='cs5';
		text.font.name='Arial';
		text.font.size='9';
	}
	CHAR_STYLE={
		style.name='Page Header Font';
		style.id='cs6';
		text.font.name='Arial';
		text.font.style.italic='true';
	}
	CHAR_STYLE={
		style.name='Page Number Small';
		style.id='cs7';
		text.font.name='Courier New';
		text.font.size='8';
	}
	CHAR_STYLE={
		style.name='Property Text';
		style.id='cs8';
		text.font.name='Verdana';
		text.font.size='8';
		par.lineHeight='11';
	}
	PAR_STYLE={
		style.name='Property Title';
		style.id='s8';
		text.font.size='8';
		text.font.style.bold='true';
		par.lineHeight='11';
		par.margin.right='7';
	}
	PAR_STYLE={
		style.name='Property Value';
		style.id='s9';
		text.font.name='Verdana';
		text.font.size='8';
		par.lineHeight='11';
	}
</STYLES>
<PAGE_HEADER>
	<AREA_SEC>
		FMT={
			text.style='cs6';
			table.cellpadding.both='0';
			table.border.style='none';
			table.border.bottom.style='solid';
		}
		<AREA>
			<CTRL_GROUP>
				FMT={
					par.border.bottom.style='solid';
				}
				<CTRLS>
					<LABEL>
						TEXT='attribute'
					</LABEL>
					<DATA_CTRL>
						FMT={
							text.font.style.italic='true';
						}
						FORMULA='\'"\' + getParam("qName") + \'"\''
					</DATA_CTRL>
				</CTRLS>
			</CTRL_GROUP>
		</AREA>
	</AREA_SEC>
</PAGE_HEADER>
<ROOT>
	<AREA_SEC>
		FMT={
			par.style='s6';
		}
		<AREA>
			<CTRL_GROUP>
				<CTRLS>
					<LABEL>
						TEXT='attribute'
					</LABEL>
					<DATA_CTRL>
						FMT={
							text.font.style.italic='true';
						}
						FORMULA='\'"\' + getParam("qName") + \'"\''
					</DATA_CTRL>
				</CTRLS>
			</CTRL_GROUP>
		</AREA>
	</AREA_SEC>
	<TEMPLATE_CALL>
		DESCR='Attribute Profile'
		COND='getBooleanParam("doc.attribute.profile")'
		TEMPLATE_FILE='attributeProfile.tpl'
		OUTPUT_TYPE='included'
		DSM_MODE='pass-current-model'
	</TEMPLATE_CALL>
	<TEMPLATE_CALL>
		DESCR='XML Representation Summary'
		COND='getBooleanParam("doc.attribute.xmlRep")'
		FMT={
			sec.spacing.before='12';
			sec.spacing.after='12';
		}
		TEMPLATE_FILE='../misc/xmlRep.tpl'
		OUTPUT_TYPE='included'
		DSM_MODE='pass-current-model'
	</TEMPLATE_CALL>
	<FOLDER>
		DESCR='Usage locations'
		COND='getBooleanParam("doc.attribute.list.usage") &&\ngetIntParam("usageCount") > 0'
		FMT={
			sec.outputStyle='list';
			list.item.margin.top='10';
			list.item.margin.bottom='10';
		}
		<HTARGET>
			TARGET_KEYS={
				'contextElement.id';
				'"usage-locations"';
			}
		</HTARGET>
		COLLAPSED
		<BODY>
			<FOLDER>
				DESCR='in attributeGroups'
				COLLAPSED
				<BODY>
					<ELEMENT_ITER>
						FMT={
							sec.outputStyle='list';
							list.type='delimited';
						}
						TARGET_ET='xs:%attribute'
						SCOPE='custom'
						ELEMENT_ENUM_EXPR='findElementsByKey ("attribute-usage", getParam("qName"))'
						FILTER='findPredecessorByType("xs:%element;xs:attributeGroup").instanceOf ("xs:attributeGroup")'
						SORTING='by-expr'
						SORTING_KEY={expr='findPredecessorByType("xs:attributeGroup").callStockSection("QName")',ascending,case_sensitive}
						<BODY>
							<AREA_SEC>
								<AREA>
									<CTRL_GROUP>
										FMT={
											txtfl.delimiter.type='nbsp';
										}
										<CTRLS>
											<SS_CALL_CTRL>
												SS_NAME='QName'
												PASSED_ELEMENT_EXPR='findPredecessorByType("xs:attributeGroup")'
												PASSED_ELEMENT_MATCHING_ET='xs:attributeGroup'
											</SS_CALL_CTRL>
											<PANEL>
												COND='! output.format.supportsPagination &&\n  hyperTargetExists (Array (contextElement.id, "usage"))'
												FMT={
													ctrl.size.width='66';
													txtfl.delimiter.type='none';
												}
												<AREA>
													<CTRL_GROUP>
														<CTRLS>
															<LABEL>
																TEXT='['
															</LABEL>
															<LABEL>
																<DOC_HLINK>
																	TARGET_KEYS={
																		'contextElement.id';
																		'"usage"';
																	}
																</DOC_HLINK>
																TEXT='ref'
															</LABEL>
															<LABEL>
																TEXT=']'
															</LABEL>
														</CTRLS>
													</CTRL_GROUP>
												</AREA>
											</PANEL>
											<PANEL>
												COND='output.format.supportsPagination &&\ngetBooleanParam("page.refs") &&\nhyperTargetExists (Array (contextElement.id, "usage"))'
												FMT={
													ctrl.size.width='158.3';
													text.style='cs7';
													txtfl.delimiter.type='none';
												}
												<AREA>
													<CTRL_GROUP>
														<CTRLS>
															<LABEL>
																TEXT='['
															</LABEL>
															<DATA_CTRL>
																FMT={
																	ctrl.option.noHLinkFmt='true';
																	text.hlink.fmt='none';
																}
																<DOC_HLINK>
																	TARGET_KEYS={
																		'contextElement.id';
																		'"usage"';
																	}
																</DOC_HLINK>
																DOCFIELD='page-htarget'
															</DATA_CTRL>
															<LABEL>
																TEXT=']'
															</LABEL>
														</CTRLS>
													</CTRL_GROUP>
												</AREA>
											</PANEL>
										</CTRLS>
									</CTRL_GROUP>
								</AREA>
							</AREA_SEC>
						</BODY>
						<HEADER>
							<AREA_SEC>
								FMT={
									par.style='s5';
								}
								<HTARGET>
									TARGET_KEYS={
										'contextElement.id';
										'"usage-locations"';
									}
								</HTARGET>
								<AREA>
									<CTRL_GROUP>
										<CTRLS>
											<LABEL>
												TEXT='In definitions of other attributeGroups'
											</LABEL>
											<DATA_CTRL>
												FORMULA='"(" + iterator.numItems + ")"'
											</DATA_CTRL>
											<DELIMITER>
												FMT={
													txtfl.delimiter.type='none';
												}
											</DELIMITER>
											<LABEL>
												TEXT=':'
											</LABEL>
										</CTRLS>
									</CTRL_GROUP>
								</AREA>
							</AREA_SEC>
						</HEADER>
					</ELEMENT_ITER>
				</BODY>
			</FOLDER>
			<FOLDER>
				DESCR='in global complexTypes'
				COLLAPSED
				<BODY>
					<ELEMENT_ITER>
						FMT={
							sec.outputStyle='list';
							list.type='delimited';
						}
						TARGET_ET='xs:%attribute'
						SCOPE='custom'
						ELEMENT_ENUM_EXPR='findElementsByKey ("attribute-usage", getParam("qName"))'
						FILTER='findPredecessorByType("xs:%element;xs:complexType").instanceOf ("xs:complexType")'
						SORTING='by-expr'
						SORTING_KEY={expr='findPredecessorByType("xs:complexType").callStockSection("QName")',ascending,case_sensitive}
						<BODY>
							<AREA_SEC>
								<AREA>
									<CTRL_GROUP>
										FMT={
											txtfl.delimiter.type='nbsp';
										}
										<CTRLS>
											<SS_CALL_CTRL>
												SS_NAME='QName'
												PASSED_ELEMENT_EXPR='findPredecessorByType("xs:complexType")'
												PASSED_ELEMENT_MATCHING_ET='xs:complexType'
											</SS_CALL_CTRL>
											<PANEL>
												COND='! output.format.supportsPagination &&\n  hyperTargetExists (Array (contextElement.id, "usage"))'
												FMT={
													ctrl.size.width='66';
													txtfl.delimiter.type='none';
												}
												<AREA>
													<CTRL_GROUP>
														<CTRLS>
															<LABEL>
																TEXT='['
															</LABEL>
															<LABEL>
																<DOC_HLINK>
																	TARGET_KEYS={
																		'contextElement.id';
																		'"usage"';
																	}
																</DOC_HLINK>
																TEXT='ref'
															</LABEL>
															<LABEL>
																TEXT=']'
															</LABEL>
														</CTRLS>
													</CTRL_GROUP>
												</AREA>
											</PANEL>
											<PANEL>
												COND='output.format.supportsPagination &&\ngetBooleanParam("page.refs") &&\nhyperTargetExists (Array (contextElement.id, "usage"))'
												FMT={
													ctrl.size.width='159.8';
													text.style='cs7';
													txtfl.delimiter.type='none';
												}
												<AREA>
													<CTRL_GROUP>
														<CTRLS>
															<LABEL>
																TEXT='['
															</LABEL>
															<DATA_CTRL>
																FMT={
																	ctrl.option.noHLinkFmt='true';
																	text.hlink.fmt='none';
																}
																<DOC_HLINK>
																	TARGET_KEYS={
																		'contextElement.id';
																		'"usage"';
																	}
																</DOC_HLINK>
																DOCFIELD='page-htarget'
															</DATA_CTRL>
															<LABEL>
																TEXT=']'
															</LABEL>
														</CTRLS>
													</CTRL_GROUP>
												</AREA>
											</PANEL>
										</CTRLS>
									</CTRL_GROUP>
								</AREA>
							</AREA_SEC>
						</BODY>
						<HEADER>
							<AREA_SEC>
								FMT={
									par.style='s5';
								}
								<HTARGET>
									TARGET_KEYS={
										'contextElement.id';
										'"usage-locations"';
									}
								</HTARGET>
								<AREA>
									<CTRL_GROUP>
										<CTRLS>
											<LABEL>
												TEXT='In definitions of global complexTypes'
											</LABEL>
											<DATA_CTRL>
												FORMULA='"(" + iterator.numItems + ")"'
											</DATA_CTRL>
											<DELIMITER>
												FMT={
													txtfl.delimiter.type='none';
												}
											</DELIMITER>
											<LABEL>
												TEXT=':'
											</LABEL>
										</CTRLS>
									</CTRL_GROUP>
								</AREA>
							</AREA_SEC>
						</HEADER>
					</ELEMENT_ITER>
				</BODY>
			</FOLDER>
			<FOLDER>
				DESCR='in embedded complexTypes of elements'
				COLLAPSED
				<BODY>
					<ELEMENT_ITER>
						FMT={
							sec.outputStyle='list';
							list.type='delimited';
						}
						TARGET_ET='xs:%attribute'
						SCOPE='custom'
						ELEMENT_ENUM_EXPR='findElementsByKey ("attribute-usage", getParam("qName"))'
						FILTER='findPredecessorByType("xs:%element") != null'
						SORTING='by-expr'
						SORTING_KEY={expr='findPredecessorByType("xs:%element").callStockSection("Element Location")',ascending,case_sensitive}
						<BODY>
							<AREA_SEC>
								<AREA>
									<CTRL_GROUP>
										FMT={
											txtfl.delimiter.type='nbsp';
										}
										<CTRLS>
											<SS_CALL_CTRL>
												SS_NAME='Element Location'
												PASSED_ELEMENT_EXPR='findPredecessorByType("xs:%element")'
												PASSED_ELEMENT_MATCHING_ET='xs:%element'
											</SS_CALL_CTRL>
											<PANEL>
												COND='! output.format.supportsPagination &&\n  hyperTargetExists (Array (contextElement.id, "usage"))'
												FMT={
													ctrl.size.width='66';
													txtfl.delimiter.type='none';
												}
												<AREA>
													<CTRL_GROUP>
														<CTRLS>
															<LABEL>
																TEXT='['
															</LABEL>
															<LABEL>
																<DOC_HLINK>
																	TARGET_KEYS={
																		'contextElement.id';
																		'"usage"';
																	}
																</DOC_HLINK>
																TEXT='ref'
															</LABEL>
															<LABEL>
																TEXT=']'
															</LABEL>
														</CTRLS>
													</CTRL_GROUP>
												</AREA>
											</PANEL>
											<PANEL>
												COND='output.format.supportsPagination &&\ngetBooleanParam("page.refs") &&\nhyperTargetExists (Array (contextElement.id, "usage"))'
												FMT={
													ctrl.size.width='159.8';
													text.style='cs7';
													txtfl.delimiter.type='none';
												}
												<AREA>
													<CTRL_GROUP>
														<CTRLS>
															<LABEL>
																TEXT='['
															</LABEL>
															<DATA_CTRL>
																FMT={
																	ctrl.option.noHLinkFmt='true';
																	text.hlink.fmt='none';
																}
																<DOC_HLINK>
																	TARGET_KEYS={
																		'contextElement.id';
																		'"usage"';
																	}
																</DOC_HLINK>
																DOCFIELD='page-htarget'
															</DATA_CTRL>
															<LABEL>
																TEXT=']'
															</LABEL>
														</CTRLS>
													</CTRL_GROUP>
												</AREA>
											</PANEL>
										</CTRLS>
									</CTRL_GROUP>
								</AREA>
							</AREA_SEC>
						</BODY>
						<HEADER>
							<AREA_SEC>
								FMT={
									par.style='s5';
								}
								<HTARGET>
									TARGET_KEYS={
										'contextElement.id';
										'"usage-locations"';
									}
								</HTARGET>
								<AREA>
									<CTRL_GROUP>
										<CTRLS>
											<LABEL>
												TEXT='In definitions of embedded complexTypes of elements'
											</LABEL>
											<DATA_CTRL>
												FORMULA='"(" + iterator.numItems + ")"'
											</DATA_CTRL>
											<DELIMITER>
												FMT={
													txtfl.delimiter.type='none';
												}
											</DELIMITER>
											<LABEL>
												TEXT=':'
											</LABEL>
										</CTRLS>
									</CTRL_GROUP>
								</AREA>
							</AREA_SEC>
						</HEADER>
					</ELEMENT_ITER>
				</BODY>
			</FOLDER>
		</BODY>
		<HEADER>
			<AREA_SEC>
				FMT={
					par.style='s2';
				}
				<AREA>
					<CTRL_GROUP>
						FMT={
							trow.bkgr.color='#CCCCFF';
						}
						<CTRLS>
							<LABEL>
								TEXT='Known Usage Locations'
							</LABEL>
						</CTRLS>
					</CTRL_GROUP>
				</AREA>
			</AREA_SEC>
		</HEADER>
	</FOLDER>
	<FOLDER>
		DESCR='Annotation'
		COND='getBooleanParam("doc.attribute.annotation")'
		COLLAPSED
		<BODY>
			<TEMPLATE_CALL>
				FMT={
					text.style='cs5';
				}
				TEMPLATE_FILE='../misc/annotation.tpl'
				OUTPUT_TYPE='included'
				DSM_MODE='pass-current-model'
			</TEMPLATE_CALL>
		</BODY>
		<HEADER>
			<AREA_SEC>
				FMT={
					par.style='s2';
				}
				<AREA>
					<CTRL_GROUP>
						FMT={
							trow.bkgr.color='#CCCCFF';
						}
						<CTRLS>
							<LABEL>
								TEXT='Annotation'
							</LABEL>
						</CTRLS>
					</CTRL_GROUP>
				</AREA>
			</AREA_SEC>
		</HEADER>
	</FOLDER>
	<FOLDER>
		DESCR='EMBEDDED TYPE DETAIL'
		COND='getBooleanParam("doc.attribute.embeddedType")'
		CONTEXT_ELEMENT_EXPR='findChild("xs:simpleType")'
		MATCHING_ET='xs:%simpleType'
		<HTARGET>
			TARGET_KEYS={
				'contextElement.id';
				'"detail"';
			}
		</HTARGET>
		COLLAPSED
		<BODY>
			<AREA_SEC>
				FMT={
					sec.outputStyle='table';
					sec.spacing.before='10';
					sec.spacing.after='10';
					table.sizing='Relative';
					table.autofit='false';
					table.cellpadding.both='5';
					table.bkgr.color='#F5F5F5';
					table.border.style='solid';
					table.border.color='#999999';
					table.page.keepTogether='true';
					table.option.borderStylesInHTML='true';
				}
				<AREA>
					<CTRL_GROUP>
						<CTRLS>
							<TEMPLATE_CALL_CTRL>
								FMT={
									ctrl.size.width='499.5';
								}
								TEMPLATE_FILE='../type/derivationTree.tpl'
								OUTPUT_TYPE='included'
								DSM_MODE='pass-current-model'
							</TEMPLATE_CALL_CTRL>
						</CTRLS>
					</CTRL_GROUP>
				</AREA>
			</AREA_SEC>
			<TEMPLATE_CALL>
				TEMPLATE_FILE='../misc/annotation.tpl'
				PASSED_PARAMS={
					'showHeading','true';
				}
				OUTPUT_TYPE='included'
				DSM_MODE='pass-current-model'
			</TEMPLATE_CALL>
			<TEMPLATE_CALL>
				DESCR='type definition info'
				FMT={
					sec.spacing.before='8';
				}
				TEMPLATE_FILE='../type/simpleContentDef.tpl'
				OUTPUT_TYPE='included'
				DSM_MODE='pass-current-model'
			</TEMPLATE_CALL>
		</BODY>
		<HEADER>
			<AREA_SEC>
				FMT={
					par.style='s1';
				}
				<AREA>
					<CTRL_GROUP>
						FMT={
							trow.bkgr.color='#CCCCFF';
						}
						<CTRLS>
							<LABEL>
								TEXT='Embedded Type Detail'
							</LABEL>
						</CTRLS>
					</CTRL_GROUP>
				</AREA>
			</AREA_SEC>
		</HEADER>
	</FOLDER>
	<FOLDER>
		DESCR='XML SOURCE'
		COND='getBooleanParam("doc.attribute.xmlSource")'
		<HTARGET>
			TARGET_KEYS={
				'contextElement.id';
				'"xml-source"';
			}
		</HTARGET>
		COLLAPSED
		<BODY>
			<AREA_SEC>
				COND='getBooleanParam("sec.xmlSource.box.component")'
				FMT={
					sec.outputStyle='table';
					table.sizing='Relative';
					table.autofit='false';
					table.cellpadding.both='5';
					table.bkgr.color='#F5F5F5';
					table.border.style='solid';
					table.border.color='#999999';
					table.option.borderStylesInHTML='true';
				}
				<AREA>
					<CTRL_GROUP>
						<CTRLS>
							<TEMPLATE_CALL_CTRL>
								FMT={
									ctrl.size.width='499.5';
									ctrl.size.height='17.3';
								}
								TEMPLATE_FILE='../misc/nodeSource.tpl'
								OUTPUT_TYPE='included'
								DSM_MODE='pass-current-model'
							</TEMPLATE_CALL_CTRL>
						</CTRLS>
					</CTRL_GROUP>
				</AREA>
			</AREA_SEC>
			<TEMPLATE_CALL>
				COND='! getBooleanParam("sec.xmlSource.box.component")'
				TEMPLATE_FILE='../misc/nodeSource.tpl'
				OUTPUT_TYPE='included'
				DSM_MODE='pass-current-model'
			</TEMPLATE_CALL>
		</BODY>
		<HEADER>
			<AREA_SEC>
				FMT={
					par.style='s1';
				}
				<AREA>
					<CTRL_GROUP>
						FMT={
							trow.bkgr.color='#CCCCFF';
						}
						<CTRLS>
							<LABEL>
								TEXT='XML Source'
							</LABEL>
						</CTRLS>
					</CTRL_GROUP>
				</AREA>
			</AREA_SEC>
		</HEADER>
	</FOLDER>
	<TEMPLATE_CALL>
		DESCR='Bottom Message'
		COND='output.type == "document"'
		TEMPLATE_FILE='../about.tpl'
		OUTPUT_TYPE='included'
		DSM_MODE='pass-current-model'
	</TEMPLATE_CALL>
</ROOT>
<STOCK_SECTIONS>
	<AREA_SEC>
		MATCHING_ET='xs:%element'
		FMT={
			sec.outputStyle='text-par';
			txtfl.delimiter.type='none';
		}
		SS_NAME='Element Location'
		<AREA>
			<CTRL_GROUP>
				<CTRLS>
					<SS_CALL_CTRL>
						SS_NAME='QName'
					</SS_CALL_CTRL>
					<TEMPLATE_CALL_CTRL>
						MATCHING_ET='xs:%localElement'
						TEMPLATE_FILE='../element/localElementExt.tpl'
						OUTPUT_TYPE='included'
						DSM_MODE='pass-current-model'
					</TEMPLATE_CALL_CTRL>
				</CTRLS>
			</CTRL_GROUP>
		</AREA>
	</AREA_SEC>
	<AREA_SEC>
		FMT={
			par.option.nowrap='true';
		}
		SS_NAME='QName'
		<AREA>
			<CTRL_GROUP>
				<CTRLS>
					<DATA_CTRL>
						<DOC_HLINK>
							TARGET_KEYS={
								'contextElement.id';
								'"detail"';
							}
						</DOC_HLINK>
						FORMULA='name = getAttrStringValue("name");\n\nschema = getXMLDocument().findChild ("xs:schema");\nnsURI = schema.getAttrStringValue("targetNamespace");\n\ninstanceOf ("xs:%localElement") ? \n{\n  ((form = getAttrStringValue("form")) == "") ? {\n    form = schema.getAttrStringValue ("elementFormDefault");\n  };\n\n  (form != "qualified") ? name : QName (nsURI, name)\n} \n: QName (nsURI, name, Enum (rootElement, contextElement))'
					</DATA_CTRL>
				</CTRLS>
			</CTRL_GROUP>
		</AREA>
	</AREA_SEC>
</STOCK_SECTIONS>
CHECKSUM='yrc?43mEJDrjmQIEaqss1w'
</DOCFLEX_TEMPLATE>